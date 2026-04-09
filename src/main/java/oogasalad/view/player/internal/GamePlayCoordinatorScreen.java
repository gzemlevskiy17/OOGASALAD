package oogasalad.view.player.internal;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.internal.GameplaySession;
import oogasalad.view.core.api.AppScreen;

// Keep GamePlayAppScreen view-only
public class GamePlayCoordinatorScreen implements AppScreen {

  private final Consumer<Outcome> onOutcome;
  private final GameplaySessionFactory sessionFactory;
  private final GamePlayAppScreen view;

  private GameplaySession session;

  public GamePlayCoordinatorScreen(Consumer<Outcome> onOutcome, GameplaySessionFactory sessionFactory) {
    this.onOutcome = onOutcome;
    this.sessionFactory = sessionFactory;
    this.view = new GamePlayAppScreen(this::handleOutcome);
  }

}
