package oogasalad.controller.player.internal;

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
}