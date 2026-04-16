package oogasalad.controller.player.internal;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Coordinates the gameplay screen lifecycle and delegates simulation control to a
 * {@link GameplaySession}. This class bridges UI events (pause/reset/back) to controller
 * actions while exposing a single {@link AppScreen} to the screen manager.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */

// Keep GamePlayAppScreen view-only
public class GamePlayScreenCoordinator implements AppScreen {

  private final Consumer<Outcome> onOutcome;
  private final GameplaySessionFactory sessionFactory;
  private final Supplier<VehicleLayout> vehicleLayoutSupplier;
  private final GamePlayAppScreen view;

  private GameplaySession session;

  /**
   * Creates a coordinator for one gameplay screen instance.
   *
   * @param onOutcome callback invoked when gameplay emits a terminal navigation/result outcome
   * @param sessionFactory factory that assembles a new gameplay session on screen entry
   * @param vehicleLayoutSupplier called on screen entry to get the current vehicle layout
   */
  public GamePlayScreenCoordinator(Consumer<Outcome> onOutcome,
      GameplaySessionFactory sessionFactory, Supplier<VehicleLayout> vehicleLayoutSupplier) {
    this.onOutcome = onOutcome;
    this.sessionFactory = sessionFactory;
    this.vehicleLayoutSupplier = vehicleLayoutSupplier;
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
    VehicleLayout layout = vehicleLayoutSupplier.get();
    session = sessionFactory.create(view, this::handleOutcome, layout);
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
