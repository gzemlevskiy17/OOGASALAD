package oogasalad.controller.player.internal;

import java.util.function.Consumer;
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
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.player.api.LossConditionChecker;
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
 */
public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  // Constants for a demonstration
  private static final double BALL_RADIUS = 12.0;
  private static final double WORLD_WIDTH = 2600.0;
  private static final double WORLD_HEIGHT = 1600.0;

  private final LevelLoader levelLoader;
  private final String levelPath;
  private final VehicleAssemblyProvider vehicleAssemblyProvider;
  private final VehicleFactory vehicleFactory;

  /**
   * @param levelLoader loads JSON level data into the world
   * @param levelPath filesystem path passed to LevelLoader
   */
  public GameplaySessionFactoryImpl(LevelLoader levelLoader, String levelPath,
      VehicleAssemblyProvider vehicleAssemblyProvider, VehicleFactory vehicleFactory) {
    if (levelLoader == null) {
      throw new IllegalArgumentException("levelLoader must not be null");
    }
    if (levelPath == null) {
      throw new IllegalArgumentException("levelPath must not be null");
    }
    if (vehicleAssemblyProvider == null) {
      throw new IllegalArgumentException("vehicleAssemblyProvider must not be null");
    }
    if (vehicleFactory == null) {
      throw new IllegalArgumentException("vehicleFactory must not be null");
    }
    this.levelLoader = levelLoader;
    this.levelPath = levelPath;
    this.vehicleAssemblyProvider = vehicleAssemblyProvider;
    this.vehicleFactory = vehicleFactory;
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
   *
   * @param screen gameplay view implementation used for render-layer attachment and sizing
   * @param onOutcome callback fired when controller detects terminal game outcomes
   * @return fully assembled gameplay session ready to start
   */
  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome) {
    // Re-entry / retry can reuse the same view instance, so clear stale nodes from prior sessions.
    screen.getGameLayer().getChildren().clear();

    GameConfig config = buildConfig();
    World world = buildWorldForCurrentLevel(config, buildWorldForCurrentLevel(config, vehicleAssemblyProvider.getCurrentAssembly()));
    GameEngine engine = buildEngine(world);

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker();
    InputRouter inputRouter = buildInputRouter();

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

    CameraBehavior behavior = new FollowPlayerCameraBehavior(() -> player, viewWidth, viewHeight, 0, 0, 0.2);
    camera.setBehavior(behavior);
    SpriteCache spriteCache = new SpriteCache();
    Renderer objectRenderer = new RendererImpl(screen.getGameLayer(), r -> {
      if (r instanceof SpriteRenderable sr) {
        return createImageView(sr, spriteCache);
      }
      Circle c = new Circle(BALL_RADIUS);
      if (r == player) {
        c.setRadius(BALL_RADIUS + 3);
        c.setFill(Color.ORANGE); // followed ball
      } else if (r instanceof GameObject go) {
        Vector v = go.getState().getLinearVelocity();
        boolean moving = Math.hypot(v.x(), v.y()) > 0.001;
        c.setFill(moving ? Color.DODGERBLUE : Color.DARKGRAY); // moving vs idle
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

  private World buildWorldForCurrentLevel(GameConfig config, VehicleAssembly vehicle) {
    World world = new WorldImpl();
    levelLoader.loadLevel(levelPath, world, config);

    VehicleAssembly assembly = vehicleAssemblyProvider.getCurrentAssembly();
    VehicleBuild build = vehicleFactory.build(assembly, 100.0, 100.0); // spawn placeholders for now
    for (GameComponent part : build.components()) {
      world.addComponent(part);
    }
    for (Joint joint : build.joints()) {
      world.addJoint(joint);
    }
    return world
  }

  private GameComponent createMovingCircle(double x, double y, double vx, double vy) {
    DynamicComponent c = new DynamicComponent(x, y, 0, 1.0, 0.1) {
    };
    c.setAffectedByGravity(false);
    c.getState().setLinearVelocity(new Vector(vx, vy));
    return c;
  }

  private GameEngine buildEngine(World world) {
    final double minX = BALL_RADIUS;
    final double maxX = WORLD_WIDTH - BALL_RADIUS;
    final double minY = BALL_RADIUS;
    final double maxY = WORLD_HEIGHT - BALL_RADIUS;

    return elapsedTime -> {
      for (Joint joint : world.getJoints()) {
        joint.apply(elapsedTime);
      }
      for (GameObject obj : world.getObjects()) {
        obj.getState().integrateObjectState(elapsedTime);
        double x = obj.getState().getLinearPosition().x();
        double y = obj.getState().getLinearPosition().y();
        double vx = obj.getState().getLinearVelocity().x();
        double vy = obj.getState().getLinearVelocity().y();
        if (x < minX || x > maxX) vx = -vx;
        if (y < minY || y > maxY) vy = -vy;
        obj.getState().setLinearPosition(new Vector(
            Math.max(minX, Math.min(maxX, x)),
            Math.max(minY, Math.min(maxY, y))
        ));
        obj.getState().setLinearVelocity(new Vector(vx, vy));
      }
    };
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

  // Hierararchy of objects to follow
  private GameObject resolveControlledPlayer(World world) {
    GameObject pig = world.getObjectsByTag(DefaultTag.PIGS.value()).stream().findFirst().orElse(null);
    if (pig != null) return pig;

    GameObject vehicle = world.getObjectsByTag(DefaultTag.VEHICLE.value()).stream().findFirst().orElse(null);
    if (vehicle != null) return vehicle;

    GameObject dynamic = world.getObjectsByTag(DefaultTag.DYNAMIC.value()).stream().findFirst().orElse(null);
    if (dynamic != null) return dynamic;

    return world.getObjects().stream().findFirst().orElse(null);
  }

}
