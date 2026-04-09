package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
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
    // Runtime dependencies from existing engines
    World world = buildWorldForCurrentLevel();
    GameEngine engine = buildEngine(world);
    GameConfig config = buildConfig();

    WinConditionChecker winChecker = buildWinChecker();
    LossConditionChecker lossChecker = buildLossChecker();
    InputRouter inputRouter = buildInputRouter();

    double viewWidth = screen.getGameLayer().getPrefWidth();
    double viewHeight = screen.getGameLayer().getPrefHeight();

    if (viewWidth <= 0) {
      viewWidth = 1280;
    }

    if (viewHeight <= 0) {
      viewHeight = 720;
    }

    Camera camera = new CameraImpl(0, 0, viewWidth, viewHeight); // Change hardcoded values later
    GameObject player = resolveControlledPlayer(world);

    CameraBehavior behavior = new FollowPlayerCameraBehavior(() -> player, viewWidth, viewHeight, 0, 0, 0.2); // Replace hardcoded values later
    camera.setBehavior(behavior);

    Renderer renderer = new RendererImpl(screen.getGameLayer());

    GameController controller = new GameControllerImpl(
        engine, world, renderer, camera, behavior, winChecker, lossChecker, inputRouter, config
    );

    controller.setOnOutcome(onOutcome);

    return new GameplaySession(controller);
  }

  private World buildWorldForCurrentLevel() { throw new UnsupportedOperationException(); }
  private GameEngine buildEngine(World world) { throw new UnsupportedOperationException(); }
  private GameConfig buildConfig() { throw new UnsupportedOperationException(); }
  private WinConditionChecker buildWinChecker() { throw new UnsupportedOperationException(); }
  private LossConditionChecker buildLossChecker() { throw new UnsupportedOperationException(); }
  private InputRouterImpl buildInputRouter() { throw new UnsupportedOperationException(); }
  private GameObject resolveControlledPlayer(World world) { throw new UnsupportedOperationException(); }

}
