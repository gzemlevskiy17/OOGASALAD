package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.player.internal.GamePlayAppScreen;

// Keep GamePlayAppScreen view-only
public class GamePlayScreenCoordinator implements AppScreen {

  private final Consumer<Outcome> onOutcome;
  private final GameplaySessionFactory sessionFactory;
  private final GamePlayAppScreen view;

  private GameplaySession session;

  public GamePlayScreenCoordinator(Consumer<Outcome> onOutcome, GameplaySessionFactory sessionFactory) {
    this.onOutcome = onOutcome;
    this.sessionFactory = sessionFactory;
    this.view = new GamePlayAppScreen(this::handleOutcome); // In the future could be replaced with the GamePlayView abstraction@

    view.setOnPause(() -> {
      if (session != null) session.pause();
    });
    view.setOnReset(() -> {
      if (session != null) session.reset();
    });
  }

  @Override
  public Parent getRoot() {
    return view.getRoot();
  }

  @Override
  public void onEnter() {
    session = sessionFactory.create(view, this::handleOutcome);
    session.start();
  }

  private void handleOutcome(Outcome outcome) {
    if (session != null) {
      session.stop();
      session = null;
    }
    onOutcome.accept(outcome);
  }

}
