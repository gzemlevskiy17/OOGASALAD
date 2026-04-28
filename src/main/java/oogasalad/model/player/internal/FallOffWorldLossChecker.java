package oogasalad.model.player.internal;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.LossConditionChecker;

/**
 * The player loses when every "tracked" object has fallen past the world floor.
 *
 * <p>Tracked objects are pigs and, when no pig is present in the level, the controlled
 * vehicle. A pig-less level (common during early level authoring) used to leave the
 * checker permanently returning {@code false}, which meant a vehicle falling into open
 * air never ended the session — the loop kept running, the camera stayed clamped at the
 * top of the world, and the player was left staring at an empty viewport that appeared
 * black. Falling back to the vehicle closes that hole.
 *
 * @author Paulina Vvedenskaya
 */
public class FallOffWorldLossChecker implements LossConditionChecker {

  private final double worldHeight;
  private final List<String> trackedMainCharacterIds;

  public FallOffWorldLossChecker(double worldHeight) {
    this(worldHeight, List.of());
  }

  public FallOffWorldLossChecker(double worldHeight, List<String> trackedMainCharacterIds) {
    this.worldHeight = worldHeight;
    this.trackedMainCharacterIds = trackedMainCharacterIds == null
        ? List.of() : List.copyOf(trackedMainCharacterIds);
  }

  @Override
  public boolean hasLost(World world) {
    List<GameObject> tracked = collectTracked(world);
    if (tracked.isEmpty()) {
      return false;
    }
    for (GameObject obj : tracked) {
      if (obj.y() <= worldHeight) {
        return false;
      }
    }
    return true;
  }

  private List<GameObject> collectTracked(World world) {
    if (!trackedMainCharacterIds.isEmpty()) {
      List<GameObject> tracked = new ArrayList<>();
      for (String id : trackedMainCharacterIds) {
        if (id != null && !id.isBlank()) {
          tracked.addAll(MainCharacterLookup.find(world, id));
        }
      }
      return tracked;
    }
    List<GameObject> tracked = new ArrayList<>(world.getObjectsByTag(DefaultTag.PIGS.value()));
    if (tracked.isEmpty()) {
      tracked.addAll(world.getObjectsByTag(DefaultTag.VEHICLE.value()));
    }
    return tracked;
  }
}
