package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Coordinates the gameplay screen lifecycle and delegates simulation control to a
 * {@link GameplaySession}. This class bridges UI events (pause/reset/back) to controller
 * actions while exposing a single {@link AppScreen} to the screen manager.
 *
 * @author Iurii Beliaev
 */

// Keep GamePlayAppScreen view-only
public class GamePlayScreenCoordinator implements AppScreen {

  private final Consumer<Outcome> onOutcome;
  private final GameplaySessionFactory sessionFactory;
  private final GamePlayAppScreen view;

  private GameplaySession session;

  /**
   * Creates a coordinator for one gameplay screen instance.
   *
   * @param onOutcome callback invoked when gameplay emits a terminal navigation/result outcome
   * @param sessionFactory factory that assembles a new gameplay session on screen entry
   */
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

  /**
   * Returns the JavaFX root for the gameplay view.
   *
   * @return root node for this screen
   */
  @Override
  public Parent getRoot() {
    return view.getRoot();
  }

  /**
   * Called when this screen becomes active. Creates a fresh session and starts simulation.
   */
  @Override
  public void onEnter() {
    session = sessionFactory.create(view, this::handleOutcome);
    session.start();
  }

  /**
   * Handles game outcome events by stopping the current session and forwarding the result upstream.
   *
   * @param outcome outcome emitted by gameplay or UI actions
   */
  private void handleOutcome(Outcome outcome) {
    if (session != null) {
      session.stop();
      session = null;
    }
    onOutcome.accept(outcome);
  }

}
