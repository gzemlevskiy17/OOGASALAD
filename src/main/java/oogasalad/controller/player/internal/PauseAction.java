package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;

/**
 * Toggles the simulation between paused and running.
 *
 * @author Paulina Vvedenskaya
 */
public class PauseAction implements Action {

  private final GameController gameController;
  private boolean paused = false;

  public PauseAction(GameController gameController) {
    if (gameController == null) {
      throw new IllegalArgumentException("gameController must not be null");
    }
    this.gameController = gameController;
  }

  @Override
  public void execute() {
    if (paused) {
      gameController.startSimulation();
      paused = false;
    } else {
      gameController.pauseSimulation();
      paused = true;
    }
  }
}
