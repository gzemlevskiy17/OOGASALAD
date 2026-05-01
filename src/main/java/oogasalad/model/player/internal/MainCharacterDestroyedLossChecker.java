package oogasalad.model.player.internal;

import java.util.List;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.LossConditionChecker;

/**
 * The player loses when any main-character object's HP reaches zero.
 *
 * <p>Uses {@link MainCharacterLookup} to find the tracked objects the same way
 * {@link FallOffWorldLossChecker} does, then checks each one that implements {@link Damageable}. If
 * none of the tracked objects are damageable the checker never fires — this avoids false positives
 * in levels where the main character has no HP system.
 *
 * @author Paulina Vvedenskaya
 */
public class MainCharacterDestroyedLossChecker implements LossConditionChecker {

  private final String mainCharacterId;

  public MainCharacterDestroyedLossChecker(String mainCharacterId) {
    this.mainCharacterId = mainCharacterId;
  }

  @Override
  public boolean hasLost(World world) {
    List<GameObject> tracked = MainCharacterLookup.find(world, mainCharacterId);
    for (GameObject obj : tracked) {
      if (obj instanceof Damageable damageable && damageable.isDestroyed()) {
        return true;
      }
    }
    return false;
  }
}
