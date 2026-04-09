package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.GameController;

// Wrapper for one Gameplay run
public class GameplaySession {
  private final GameController controller;

  public GameplaySession(GameController controller) {
    this.controller = controller;
  }

  public void start() { controller.startSimulation(); }
  public void pause() { controller.pauseSimulation(); }
  public void reset() { controller.resetSimulation(); }
  public void stop()  { controller.stopSimulation(); }
}