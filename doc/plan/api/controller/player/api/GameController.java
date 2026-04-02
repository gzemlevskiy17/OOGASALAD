package oogasalad.controller.player.api;

/**
 * The heartbeat for the Playing environment.
 */
public interface GameController {
  /**
   * Starts the 60-FPS loop.
   */
  void startSimulation();

  /**
   * Pauses the physics and rendering ticks.
   */
  void pauseSimulation();
  /**
   * Ends the simulation
   */
  void stopSimulation();
}