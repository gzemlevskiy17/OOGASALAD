package oogasalad.controller.player.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.ImpulseCollisionSolver;
import oogasalad.model.engine.VehicleTerrainSupport;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.VehicleComponent;
import oogasalad.model.player.api.GameplayStatsTracker;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.model.player.internal.FallOffWorldLossChecker;
import oogasalad.model.player.internal.FinishLineWinChecker;
import oogasalad.model.player.internal.GameplayStatsTrackerImpl;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.api.Sprite;
import oogasalad.view.core.api.SpriteRegistry;
import oogasalad.view.core.api.SpriteRenderable;
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.RendererImpl;
import oogasalad.view.core.internal.SpriteCache;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Default factory that assembles the full gameplay runtime graph (world, engine, camera,
 * renderer, input, win/loss checks, and controller) for one playable session.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  private static final double MIN_VIEWPORT_SIZE = 1.0;
  private static final double DEFAULT_VIEW_WIDTH = 1280.0;
  private static final double DEFAULT_VIEW_HEIGHT = 720.0;
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.5;
  private static final double MAX_ZOOM = 3.0;
  private static final double BOUNDS_PADDING = 400.0;
  private static final double FALLBACK_WORLD_WIDTH = 2600.0;
  private static final double FALLBACK_WORLD_HEIGHT = 1600.0;

  private record LevelBounds(double width, double height) {}

  private record BuiltWorld(World world, LevelBounds bounds) {}

  private final VehicleFactory vehicleFactory = new VehicleFactory();
  private final LevelLoader levelLoader;
  private final Supplier<String> levelPathSupplier;

  public GameplaySessionFactoryImpl(
      LevelLoader levelLoader,
      Supplier<String> levelPathSupplier
  ) {
    this.levelLoader = levelLoader;
    this.levelPathSupplier = levelPathSupplier;
  }

  /**
   * Builds and wires one gameplay session using the supplied screen and outcome callback.
   */
  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome,
      VehicleLayout layout) {
    screen.getGameLayer().getChildren().clear();

    GameConfig config = buildConfig();
    BuiltWorld built = buildWorld(config, layout);
    World world = built.world();
    LevelBounds bounds = built.bounds();
    GameEngine engine = new GameEngineImpl(world, new CollisionDetectorImpl(),
        new ImpulseCollisionSolver());

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker(bounds.height());
    InputRouter inputRouter = buildInputRouter();

    bindDriveActions(inputRouter, world);
    screen.setOnToolActivated(inputRouter::handleButtonPress);
    screen.setOnToolDeactivated(inputRouter::handleButtonPress);

    double viewWidth = screen.getGameLayer().getWidth();
    double viewHeight = screen.getGameLayer().getHeight();
    if (viewWidth <= 0) viewWidth = DEFAULT_VIEW_WIDTH;
    if (viewHeight <= 0) viewHeight = DEFAULT_VIEW_HEIGHT;

    // World boundary overlay (drawn in world space, projected each frame)
    Rectangle worldBounds = new Rectangle(bounds.width(), bounds.height());
    worldBounds.setManaged(false);
    worldBounds.setFill(Color.TRANSPARENT);
    worldBounds.setStroke(Color.LIMEGREEN);
    worldBounds.setStrokeWidth(2.0);
    worldBounds.getStrokeDashArray().addAll(10.0, 8.0);

    // keep the world boundary behind balls
    screen.getGameLayer().getChildren().add(worldBounds);
    worldBounds.toBack();

    Camera camera = new CameraImpl(0, 0, viewWidth, viewHeight);
    GameObject player = resolveControlledPlayer(world);

    CameraBehavior behavior = new FollowPlayerCameraBehavior(
        () -> player, 0, 0, 0.2, bounds.width(), bounds.height());
    camera.setBehavior(behavior);
    wireResponsiveCamera(screen, camera);
    SpriteCache spriteCache = new SpriteCache();
    SpriteRegistry spriteRegistry = new PaletteSpriteRegistry();
    Map<Renderable, Sprite> spritesByRenderable = new IdentityHashMap<>();

    Renderer objectRenderer = new RendererImpl(screen.getGameLayer(),
        r -> {
          if (r instanceof TerrainBlock tb) {
            Rectangle rect = new Rectangle(tb.getWidth(), tb.getHeight());
            rect.setManaged(false);
            rect.setStrokeWidth(0.5);
            rect.setStroke(Color.BLACK);
            rect.setFill(Paint.valueOf(tb.getColor()));
            return rect;
          }
          if (r instanceof SpriteRenderable) {
            Sprite sprite = spriteRegistry.resolve(r);
            spritesByRenderable.put(r, sprite);
            return sprite.render(spriteCache);
          }
          throw new IllegalStateException(
              "No renderer for " + r.getClass().getName()
                  + " — must be TerrainBlock or SpriteRenderable");
        },
        (r, node) -> {
          Sprite sprite = spritesByRenderable.get(r);
          if (sprite == null) {
            return;
          }
          double speed = 0.0;
          if (r instanceof GameObject go) {
            Vector v = go.getState().getLinearVelocity();
            speed = Math.hypot(v.x(), v.y());
          }
          sprite.update(node, speed);
        });

    Renderer renderer = (cameraRef, objects) -> {
      objectRenderer.render(cameraRef, objects);

      // project world border into viewport using camera transform
      worldBounds.setLayoutX(-cameraRef.getX());
      worldBounds.setLayoutY(-cameraRef.getY());
    };

    GameplayStatsTracker tracker = new GameplayStatsTrackerImpl(levelPathSupplier.get());
    GameController controller = new GameControllerImpl(
        engine, world, renderer, camera, behavior, winChecker, lossChecker, inputRouter, config,
        tracker
    );
    controller.setOnOutcome(onOutcome);

    return new GameplaySession(controller);
  }

  private void wireResponsiveCamera(GamePlayAppScreen screen, Camera camera) {
    final double[] baseWidth = {Math.max(DEFAULT_VIEW_WIDTH, screen.getGameLayer().getWidth())};
    final double[] baseHeight = {Math.max(DEFAULT_VIEW_HEIGHT, screen.getGameLayer().getHeight())};
    final double[] zoom = {1.0};

    Runnable applyViewport = () -> {
      double viewWidth = Math.max(MIN_VIEWPORT_SIZE, baseWidth[0] / zoom[0]);
      double viewHeight = Math.max(MIN_VIEWPORT_SIZE, baseHeight[0] / zoom[0]);
      camera.setViewBoundaries(viewWidth, viewHeight);
    };

    screen.getGameLayer().widthProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        baseWidth[0] = newVal.doubleValue();
        applyViewport.run();
      }
    });
    screen.getGameLayer().heightProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        baseHeight[0] = newVal.doubleValue();
        applyViewport.run();
      }
    });
    screen.getGameLayer().setOnScroll(event -> {
      if (!event.isControlDown()) {
        return;
      }
      zoom[0] = event.getDeltaY() > 0 ? zoom[0] * ZOOM_STEP : zoom[0] / ZOOM_STEP;
      zoom[0] = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom[0]));
      applyViewport.run();
      event.consume();
    });

    applyViewport.run();
  }

  /**
   * Binds the on-screen tool button IDs to actions that activate or deactivate all matching
   * Driveable components in the world. A button whose ID matches a component's display name
   * (case-insensitive) toggles that component's drive state.
   */
  // package-private so unit tests can drive it directly against a constructed World.
  void bindDriveActions(InputRouter inputRouter, World world) {
    for (GameObject obj : world.getObjects()) {
      if (!(obj instanceof Driveable driveable)) {
        continue;
      }
      if (!driveable.isPlayerControllable()) {
        continue;
      }

      String id = obj.getTag();
      if (obj instanceof VehicleComponent vc) {
        id = vc.getDisplayName().toLowerCase();
      }

      inputRouter.bind(id, new DriveAction(driveable));
    }
  }

  private BuiltWorld buildWorld(GameConfig config, VehicleLayout layout) {
    World world = new WorldImpl();

    String levelPath = levelPathSupplier.get();
    if (levelPath != null && !levelPath.isBlank()) {
      levelLoader.loadLevel(levelPath, world, config);
    }

    LevelBounds bounds = computeLevelBounds(world);
    // Vehicle bodies on this branch use centered polygons, so cell (0,0)'s center lands at
    // (spawnX, spawnY). Offset by half a cell so the vehicle's top-left corner matches the
    // historical spawn origin (350, 300) used when bodies were top-left-anchored.
    vehicleFactory.buildVehicle(layout, world, 380, 330);
    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);
    return new BuiltWorld(world, bounds);
  }

  private LevelBounds computeLevelBounds(World world) {
    double maxX = 0;
    double maxY = 0;
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof TerrainBlock tb) {
        maxX = Math.max(maxX, tb.getX() + tb.getWidth());
        maxY = Math.max(maxY, tb.getY() + tb.getHeight());
      } else if (c instanceof GoalLineComponent gl) {
        maxX = Math.max(maxX, gl.getX() + gl.getWidth());
        maxY = Math.max(maxY, gl.getY() + gl.getHeight());
      }
    }
    double width = maxX > 0 ? maxX + BOUNDS_PADDING : FALLBACK_WORLD_WIDTH;
    double height = maxY > 0 ? maxY + BOUNDS_PADDING : FALLBACK_WORLD_HEIGHT;
    return new LevelBounds(width, height);
  }

  private GameConfig buildConfig() {
    return new GameConfig() {
      @Override
      public String getSetting(String key) { return ""; }

      @Override
      public double getNumericSetting(String key) { return 0.0; }
    };
  }

  private WinConditionChecker buildWinChecker() { return new FinishLineWinChecker(); }

  private LossConditionChecker buildLossChecker(double worldHeight) {
    return new FallOffWorldLossChecker(worldHeight);
  }

  private InputRouterImpl buildInputRouter() { return new InputRouterImpl(); }

  // Hierarchy of objects to follow
  private GameObject resolveControlledPlayer(World world) {
    GameObject pig = world.getObjectsByTag(DefaultTag.PIGS.value()).stream().findFirst()
        .orElse(null);
    if (pig != null) {
      return pig;
    }

    GameObject vehicle = world.getObjectsByTag(DefaultTag.VEHICLE.value()).stream().findFirst()
        .orElse(null);
    if (vehicle != null) {
      return vehicle;
    }

    GameObject dynamic = world.getObjectsByTag(DefaultTag.DYNAMIC.value()).stream().findFirst()
        .orElse(null);
    if (dynamic != null) {
      return dynamic;
    }

    return world.getObjects().stream().findFirst().orElse(null);
  }

}
