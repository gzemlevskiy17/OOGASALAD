package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.VehicleComponent;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.api.SpriteRenderable;
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
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

  private static final double BALL_RADIUS = 12.0;
  private static final double WORLD_WIDTH = 2600.0;
  private static final double WORLD_HEIGHT = 1600.0;

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

  private static ImageView createImageView(SpriteRenderable sr, SpriteCache spriteCache) {
    ImageView imageView = new ImageView(spriteCache.get(sr.getSpritePath()));
    imageView.setFitWidth(sr.getSpriteWidth());
    imageView.setFitHeight(sr.getSpriteHeight());
    imageView.setTranslateX(-sr.getSpriteWidth() / 2);
    imageView.setTranslateY(-sr.getSpriteHeight() / 2);
    return imageView;
  }

  /**
   * Builds and wires one gameplay session using the supplied screen and outcome callback.
   */
  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome,
      VehicleLayout layout) {
    screen.getGameLayer().getChildren().clear();

    GameConfig config = buildConfig();
    World world = buildWorld(config, layout);
    GameEngine engine = new GameEngineImpl(world, new CollisionDetectorImpl(),
        new ImpulseCollisionSolver());

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker();
    InputRouter inputRouter = buildInputRouter();

    bindDriveActions(inputRouter, world);

    double viewWidth = screen.getGameLayer().getPrefWidth();
    double viewHeight = screen.getGameLayer().getPrefHeight();
    if (viewWidth <= 0) viewWidth = 1280;
    if (viewHeight <= 0) viewHeight = 720;

    // World boundary overlay (drawn in world space, projected each frame)
    Rectangle worldBounds = new Rectangle(WORLD_WIDTH, WORLD_HEIGHT);
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

    CameraBehavior behavior = new FollowPlayerCameraBehavior(() -> player, viewWidth, viewHeight,
        0, 0, 0.2);
    camera.setBehavior(behavior);
    SpriteCache spriteCache = new SpriteCache();
    Renderer objectRenderer = new RendererImpl(screen.getGameLayer(), r -> {
      if (r instanceof SpriteRenderable sr) {
        return createImageView(sr, spriteCache);
      }
      if (r instanceof TerrainBlock tb) {
        Rectangle rect = new Rectangle(tb.getWidth(), tb.getHeight());
        rect.setFill(Color.DARKGREEN);
        return rect;
      }
      Circle c = new Circle(BALL_RADIUS);
      if (r == player) {
        c.setRadius(BALL_RADIUS + 3);
        c.setFill(Color.ORANGE);
      } else if (r instanceof GameObject go) {
        Vector v = go.getState().getLinearVelocity();
        boolean moving = Math.hypot(v.x(), v.y()) > 0.001;
        c.setFill(moving ? Color.DODGERBLUE : Color.DARKGRAY);
      } else {
        c.setFill(Color.DODGERBLUE);
      }
      return c;
    });

    Renderer renderer = (cameraRef, objects) -> {
      objectRenderer.render(cameraRef, objects);

      // project world border into viewport using camera transform
      worldBounds.setLayoutX(-cameraRef.getX());
      worldBounds.setLayoutY(-cameraRef.getY());
    };

    GameController controller = new GameControllerImpl(
        engine, world, renderer, camera, behavior, winChecker, lossChecker, inputRouter, config
    );
    controller.setOnOutcome(onOutcome);

    return new GameplaySession(controller);
  }

  /**
   * Binds the on-screen tool button IDs to actions that activate or deactivate all matching
   * Driveable components in the world. A button whose ID matches a component's display name
   * (case-insensitive) toggles that component's drive state.
   */
  private void bindDriveActions(InputRouter inputRouter, World world) {
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

      final Driveable target = driveable;
      final String buttonId = id;
      inputRouter.bind(buttonId, () -> {
        if (target.isDriveEnabled()) {
          target.deactivateDrive();
        } else {
          target.activateDrive();
        }
      });
    }
  }

  private World buildWorld(GameConfig config, VehicleLayout layout) {
    World world = new WorldImpl();

    String levelPath = levelPathSupplier.get();
    if (levelPath != null && !levelPath.isBlank()) {
      levelLoader.loadLevel(levelPath, world, config);
    }

    vehicleFactory.buildVehicle(layout, world, 350, 300);
    return world;
  }

  private GameConfig buildConfig() {
    return new GameConfig() {
      @Override
      public String getSetting(String key) { return ""; }

      @Override
      public double getNumericSetting(String key) { return 0.0; }
    };
  }

  private WinConditionChecker buildWinChecker() { return world -> false; }

  private LossConditionChecker buildLossChecker() { return world -> false; }

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
