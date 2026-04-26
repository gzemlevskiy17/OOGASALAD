package oogasalad.controller.player.internal;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.Settings;
import oogasalad.data.internal.LevelPaths;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.components.VehicleComponent;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.model.player.api.GameplayStatsTracker;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.PlacedComponent;
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
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.RendererImpl;
import oogasalad.view.core.internal.SpriteCache;
import oogasalad.view.player.internal.GamePlayAppScreen;
import oogasalad.view.player.internal.VehicleHpHud;

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
  // Regardless of how tiny the authored terrain is, the world must be at least one
  // viewport + padding on each axis. Without this floor, a level with just a few
  // blocks near the top of the canvas yields a world shorter than the viewport,
  // which (a) clamps the follow-camera to y=0 and (b) lets the vehicle fall past
  // the world floor before the loss checker can trigger, producing the
  // "empty/black viewport" symptom during playtesting.
  private static final double MIN_WORLD_WIDTH = DEFAULT_VIEW_WIDTH + BOUNDS_PADDING;
  private static final double MIN_WORLD_HEIGHT = DEFAULT_VIEW_HEIGHT + BOUNDS_PADDING;

  private record LevelBounds(double width, double height) {}

  /**
   * Builds a JavaFX {@link Path} tracing the spline curve at the top, dropping to the baseline
   * on each end, and closing along the baseline. Coordinates are in the component's local space.
   */
  private static Node createSplineNode(TerrainSpline ts) {
    ParabolaSpline spline = ts.getSpline();
    double originX = ts.x();
    double originY = ts.y();
    double localBaselineY = spline.baselineY() - originY;

    Path path = new Path();
    path.setManaged(false);

    Vector firstKnot = spline.knots().get(0);
    path.getElements().add(new MoveTo(firstKnot.x() - originX, firstKnot.y() - originY));
    for (int i = 0; i < spline.segmentCount(); i++) {
      Vector control = spline.controls().get(i);
      Vector next = spline.knots().get(i + 1);
      path.getElements().add(new QuadCurveTo(
          control.x() - originX, control.y() - originY,
          next.x() - originX, next.y() - originY));
    }
    Vector lastKnot = spline.knots().get(spline.knots().size() - 1);
    path.getElements().add(new LineTo(lastKnot.x() - originX, localBaselineY));
    path.getElements().add(new LineTo(firstKnot.x() - originX, localBaselineY));
    path.getElements().add(new ClosePath());

    try {
      path.setFill(Paint.valueOf(ts.getColor()));
    } catch (IllegalArgumentException e) {
      path.setFill(Color.SADDLEBROWN);
    }
    path.setStroke(Color.BLACK);
    path.setStrokeWidth(1.0);
    return path;
  }

  private final VehicleFactory vehicleFactory = new VehicleFactory();
  private final LevelLoader levelLoader;
  private final GameLoader gameLoader;
  private final Supplier<String> levelPathSupplier;

  public GameplaySessionFactoryImpl(
      LevelLoader levelLoader,
      GameLoader gameLoader,
      Supplier<String> levelPathSupplier
  ) {
    this.levelLoader = levelLoader;
    this.gameLoader = gameLoader;
    this.levelPathSupplier = levelPathSupplier;
  }

  static Map<Renderable, Renderable> wheelConnectorRotationSources(World world) {
    Map<Renderable, Renderable> sources = new IdentityHashMap<>();
    for (Joint joint : world.getJoints()) {
      GameComponent a = joint.getComponentA();
      GameComponent b = joint.getComponentB();
      if (isWheel(a) && !isWheel(b)) {
        sources.put(a, b);
      } else if (isWheel(b) && !isWheel(a)) {
        sources.put(b, a);
      }
    }
    return sources;
  }

  private static boolean isWheel(GameComponent component) {
    return component instanceof VehicleComponent vehicleComponent && vehicleComponent.isWheel();
  }

  /**
   * Builds and wires one gameplay session using the supplied screen and outcome callback.
   */
  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome,
      VehicleLayout layout) {
    screen.getGameLayer().getChildren().clear();

    Settings config = buildConfig();
    BuiltWorld built = buildWorld(layout);
    World world = built.world();
    LevelBounds bounds = built.bounds();
    GameEngine engine = new GameEngineImpl(world, new CollisionDetectorImpl());

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker(bounds.height());
    InputRouter inputRouter = buildInputRouter();

    bindDriveActions(inputRouter, world);
    screen.setOnToolActivated(inputRouter::handleButtonPress);
    screen.setOnToolDeactivated(inputRouter::handleButtonPress);
    screen.setAvailableTools(collectDriveToolIds(world));

    double viewWidth = screen.getGameLayer().getWidth();
    double viewHeight = screen.getGameLayer().getHeight();
    if (viewWidth <= 0) viewWidth = DEFAULT_VIEW_WIDTH;
    if (viewHeight <= 0) viewHeight = DEFAULT_VIEW_HEIGHT;

    Camera camera = new CameraImpl(0, 0, viewWidth, viewHeight);
    GameObject player = resolveControlledPlayer(world);

    CameraBehavior behavior = new FollowPlayerCameraBehavior(
        () -> player, 0, 0, 0.2, bounds.width(), bounds.height());
    camera.setBehavior(behavior);
    wireResponsiveCamera(screen, camera);
    SpriteCache spriteCache = new SpriteCache();
    SpriteRegistry spriteRegistry = new PaletteSpriteRegistry();
    Map<Renderable, Sprite> spritesByRenderable = new IdentityHashMap<>();
    Map<Renderable, Renderable> wheelConnectorRotationSources =
        wheelConnectorRotationSources(world);

    Renderer objectRenderer = new RendererImpl(screen.getGameLayer(),
        r -> {
          if (r instanceof TerrainSpline ts) {
            return createSplineNode(ts);
          }
          if (r instanceof TerrainBlock tb) {
            Rectangle rect = new Rectangle(tb.getWidth(), tb.getHeight());
            rect.setManaged(false);
            rect.setStrokeWidth(0.5);
            rect.setStroke(Color.BLACK);
            rect.setFill(Paint.valueOf(tb.getColor()));
            return rect;
          }
          // Non-visual markers (e.g. SpawnPointComponent) live in the world for save/load and
          // lookup but have no sprite. Return an invisible stub so the render loop doesn't abort
          // the frame, which would leave the vehicle unrendered.
          if (r instanceof SpawnPointComponent) {
            Group stub = new Group();
            stub.setManaged(false);
            stub.setVisible(false);
            return stub;
          }
          if (r instanceof TypedComponent) {
            Sprite sprite = spriteRegistry.resolve(r);
            spritesByRenderable.put(r, sprite);
            return sprite.render(spriteCache);
          }
          throw new IllegalStateException( //TODO: Refactor into the "properties" error
              "No renderer for " + r.getClass().getName()
                  + " — must be TerrainSpline, TerrainBlock, or TypedComponent");
        },
        (r, node) -> {
          Sprite sprite = spritesByRenderable.get(r);
          if (sprite == null) {
            return;
          }
          double speed = 0.0;
          if (r instanceof DynamicComponent dyn) {
            Vector v = dyn.getDynamicState().getLinearVelocity();
            speed = v.x();
          }
          sprite.update(node, speed);
          Renderable connectorRotationSource = wheelConnectorRotationSources.get(r);
          if (connectorRotationSource != null) {
            node.setRotate(Math.toDegrees(connectorRotationSource.rotation()));
          }
        });

    Renderer renderer = (cameraRef, objects) -> {
      objectRenderer.render(cameraRef, objects);

    };

    GameplayStatsTracker tracker = new GameplayStatsTrackerImpl(levelPathSupplier.get());
    GameController controller = new GameControllerImpl(
        engine, world, renderer, camera, behavior, winChecker, lossChecker, inputRouter, config,
        tracker
    );
    controller.setOnOutcome(onOutcome);

    attachVehicleHpHud(screen, built.placements());

    return new GameplaySession(controller);
  }

  private void attachVehicleHpHud(GamePlayAppScreen screen,
      Map<PlacedComponent, GameComponent> placements) {
    Map<PlacedComponent, Damageable> placementHp = new LinkedHashMap<>();
    for (Map.Entry<PlacedComponent, GameComponent> entry : placements.entrySet()) {
      if (entry.getValue() instanceof Damageable d) {
        placementHp.put(entry.getKey(), d);
      }
    }
    ResourceBundle builderProps =
        ResourceBundle.getBundle("oogasalad.view.player.VehicleBuilderAppScreen");
    int rows = (int) Double.parseDouble(builderProps.getString("grid.rows"));
    int cols = (int) Double.parseDouble(builderProps.getString("grid.columns"));
    VehicleHpHud hud = new VehicleHpHud(rows, cols, placementHp);
    StackPane.setMargin(hud, new Insets(0, 0, 16, 16));
    screen.attachOverlay(hud, Pos.BOTTOM_LEFT);
  }

  private BuiltWorld buildWorld(VehicleLayout layout) {
    World world = new WorldImpl();

    String levelPath = levelPathSupplier.get();
    if (levelPath != null && !levelPath.isBlank()) {
      LevelAttributes attributes = levelLoader.loadAttributes(levelPath);
      GameConfig game = gameLoader.loadGame(
          LevelPaths.gameFileForLevel(levelPath, attributes.getGameType()));
      levelLoader.loadLevel(levelPath, game, world);
    }

    LevelBounds bounds = computeLevelBounds(world);
    SpawnPointComponent spawn = world.getAllComponents().stream()
        .filter(SpawnPointComponent.class::isInstance)
        .map(SpawnPointComponent.class::cast)
        .findFirst().orElse(null);
    double spawnX = spawn != null ? spawn.x() : 380.0;
    double spawnY = spawn != null ? spawn.y() : 330.0;
    Map<PlacedComponent, GameComponent> placements =
        vehicleFactory.buildVehicle(layout, world, spawnX, spawnY);
    return new BuiltWorld(world, bounds, placements);
  }

  private void wireResponsiveCamera(GamePlayAppScreen screen, Camera camera) {
    CameraViewportState state = new CameraViewportState(
        Math.max(DEFAULT_VIEW_WIDTH, screen.getGameLayer().getWidth()),
        Math.max(DEFAULT_VIEW_HEIGHT, screen.getGameLayer().getHeight()));

    screen.getGameLayer().widthProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        state.baseWidth = newVal.doubleValue();
        state.applyTo(camera);
      }
    });
    screen.getGameLayer().heightProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        state.baseHeight = newVal.doubleValue();
        state.applyTo(camera);
      }
    });
    screen.getGameLayer().setOnScroll(event -> {
      if (!event.isControlDown()) {
        return;
      }
      if (event.getDeltaY() > 0) {
        state.zoom = state.zoom * ZOOM_STEP;
      } else {
        state.zoom = state.zoom / ZOOM_STEP;
      }
      state.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, state.zoom));
      state.applyTo(camera);
      event.consume();
    });

    state.applyTo(camera);
  }

  private static class CameraViewportState {

    double baseWidth;
    double baseHeight;
    double zoom = 1.0;

    CameraViewportState(double baseWidth, double baseHeight) {
      this.baseWidth = baseWidth;
      this.baseHeight = baseHeight;
    }

    void applyTo(Camera camera) {
      double viewWidth = Math.max(MIN_VIEWPORT_SIZE, baseWidth / zoom);
      double viewHeight = Math.max(MIN_VIEWPORT_SIZE, baseHeight / zoom);
      camera.setViewBoundaries(viewWidth, viewHeight);
    }
  }

  Set<String> collectDriveToolIds(World world) {
    Set<String> ids = new HashSet<>();
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
      ids.add(id);
    }
    return ids;
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

  private record BuiltWorld(World world, LevelBounds bounds,
                            Map<PlacedComponent, GameComponent> placements) {

  }

  private LevelBounds computeLevelBounds(World world) {
    double maxX = 0;
    double maxY = 0;
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof TerrainBlock tb) {
        maxX = Math.max(maxX, tb.x() + tb.getWidth());
        maxY = Math.max(maxY, tb.y() + tb.getHeight());
      } else if (c instanceof TerrainSpline ts) {
        maxX = Math.max(maxX, ts.x() + ts.getWidth());
        maxY = Math.max(maxY, ts.getSpline().baselineY());
      } else if (c instanceof GoalLineComponent gl) {
        maxX = Math.max(maxX, gl.x() + gl.getWidth());
        maxY = Math.max(maxY, gl.y() + gl.getHeight());
      }
    }
    double width = Math.max(
        MIN_WORLD_WIDTH,
        maxX > 0 ? maxX + BOUNDS_PADDING : FALLBACK_WORLD_WIDTH);
    double height = Math.max(
        MIN_WORLD_HEIGHT,
        maxY > 0 ? maxY + BOUNDS_PADDING : FALLBACK_WORLD_HEIGHT);
    return new LevelBounds(width, height);
  }

  private Settings buildConfig() {
    return new Settings() {
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
