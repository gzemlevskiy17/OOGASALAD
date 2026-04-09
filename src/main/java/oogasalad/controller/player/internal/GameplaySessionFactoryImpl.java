package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.view.player.internal.GamePlayAppScreen;

public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome)) {
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
  }

}
