package oogasalad.model.player.internal;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.LossConditionChecker;

/**
 * The player loses when every pig has fallen below the world floor.
 *
 * @author Paulina Vvedenskaya
 */
public class FallOffWorldLossChecker implements LossConditionChecker {

  private final double worldHeight;

  public FallOffWorldLossChecker(double worldHeight) {
    this.worldHeight = worldHeight;
  }

  @Override
  public boolean hasLost(World world) {
    var pigs = world.getObjectsByTag(DefaultTag.PIGS.value());
    if (pigs.isEmpty()) {
      return false;
    }
    for (GameObject pig : pigs) {
      if (pig.getY() <= worldHeight) {
        return false;
      }
    }
    return true;
  }
}
