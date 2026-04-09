package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.DynamicComponent;
import oogasalad.model.engine.internal.GameComponent;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
import oogasalad.view.core.internal.RendererImpl;
import oogasalad.view.player.internal.GamePlayAppScreen;

public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome) {
    World world = buildWorldForCurrentLevel();
    GameEngine engine = buildEngine(world);
    GameConfig config = buildConfig();

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker();
    InputRouter inputRouter = buildInputRouter();

    double viewWidth = screen.getGameLayer().getPrefWidth();
    double viewHeight = screen.getGameLayer().getPrefHeight();
    if (viewWidth <= 0) viewWidth = 1280;
    if (viewHeight <= 0) viewHeight = 720;

    Camera camera = new CameraImpl(0, 0, viewWidth, viewHeight);
    GameObject player = resolveControlledPlayer(world);

    CameraBehavior behavior = new FollowPlayerCameraBehavior(() -> player, viewWidth, viewHeight, 0, 0, 0.2);
    camera.setBehavior(behavior);

    // Bigger circles so you can actually see them
    Renderer renderer = new RendererImpl(screen.getGameLayer(), r -> {
      Circle c = new Circle(12);
      c.setFill(Color.DODGERBLUE);
      return c;
    });

    GameController controller = new GameControllerImpl(
        engine, world, renderer, camera, behavior, winChecker, lossChecker, inputRouter, config
    );
    controller.setOnOutcome(onOutcome);

    return new GameplaySession(controller);
  }

  private World buildWorldForCurrentLevel() {
    World world = new WorldImpl();

    world.addComponent(createMovingCircle(200, 200,  80,  50)); // "player" camera follows this one
    world.addComponent(createMovingCircle(400, 300, -60,  70));
    world.addComponent(createMovingCircle(700, 450,  90, -80));
    world.addComponent(createMovingCircle(900, 250, -75, -65));

    return world;
  }

  private GameComponent createMovingCircle(double x, double y, double vx, double vy) {
    DynamicComponent c = new DynamicComponent(new Pose(x, y, 0), 1.0, 0.1) {};
    c.setAffectedByGravity(false);
    c.getState().setLinearVelocity(new Vector(vx, vy));
    return c;
  }

  private GameEngine buildEngine(World world) {
    final double minX = 20, maxX = 1260;
    final double minY = 20, maxY = 700;

    return elapsedTime -> {
      for (GameObject obj : world.getObjects()) {
        obj.getState().integrateObjectState(elapsedTime);

        double x = obj.getState().getLinearPosition().x();
        double y = obj.getState().getLinearPosition().y();
        double vx = obj.getState().getLinearVelocity().x();
        double vy = obj.getState().getLinearVelocity().y();

        if (x < minX || x > maxX) vx = -vx;
        if (y < minY || y > maxY) vy = -vy;

        double clampedX = Math.max(minX, Math.min(maxX, x));
        double clampedY = Math.max(minY, Math.min(maxY, y));

        obj.getState().setLinearPosition(new Vector(clampedX, clampedY));
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

  private GameObject resolveControlledPlayer(World world) {
    return world.getObjects().stream().findFirst().orElse(null);
  }

}
