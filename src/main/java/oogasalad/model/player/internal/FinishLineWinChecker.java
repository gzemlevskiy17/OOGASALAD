package oogasalad.model.player.internal;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.WinConditionChecker;

/**
 * The player wins when the pig's horizontal center reaches the finish-line zone.
 *
 * @author Paulina Vvedenskaya
 */
public class FinishLineWinChecker implements WinConditionChecker {

  @Override
  public boolean hasWon(World world) {
    for (GameObject pig : world.getObjectsByTag(DefaultTag.PIGS.value())) {
      for (GameObject finish : world.getObjectsByTag(DefaultTag.FINISH_LINE.value())) {
        if (pig.getX() >= finish.getX()) {
          return true;
        }
      }
    }
    return false;
  }
}
