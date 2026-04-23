package oogasalad.controller.player.internal;

import java.util.function.BiConsumer;
import oogasalad.controller.player.api.GameController;

/**
 * Lightweight lifecycle wrapper around a {@link GameController} for a single gameplay run.
 * It exposes semantic session controls used by the screen coordinator.
 *
 * @author Iurii Beliaev
 */
public class GameplaySession {
  private final GameController controller;

  /**
   * Creates a session bound to a concrete controller.
   *
   * @param controller gameplay controller that drives simulation lifecycle
   */
  public GameplaySession(GameController controller) {
    this.controller = controller;
  }

  /** Starts or resumes simulation. */
  public void start() { controller.startSimulation(); }

  /** Pauses simulation without discarding session state. */
  public void pause() { controller.pauseSimulation(); }

  /** Stops simulation and resets controller state as defined by the controller implementation. */
  public void reset() { controller.resetSimulation(); }

  /** Stops simulation immediately. */
  public void stop()  { controller.stopSimulation(); }

  /**
   * Forwards a tool button press to the controller's input router.
   */
  public void handleButtonPress(String buttonId) {
    controller.handleButtonPress(buttonId);
  }

  /**
   * Subscribes to per-tick {@code (elapsedSeconds, progressPercent)} updates for HUD display.
   * Pass null to disable.
   */
  public void setOnStatsTick(BiConsumer<Double, Double> callback) {
    controller.setOnStatsTick(callback);
  }
}