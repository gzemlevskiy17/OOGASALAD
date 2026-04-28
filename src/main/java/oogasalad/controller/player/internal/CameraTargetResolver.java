package oogasalad.controller.player.internal;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.VehicleComponent;

/**
 * Chooses the world object that the gameplay camera should follow.
 *
 * <p>The resolver first honors the level's {@code cameraFollows} field by matching it against
 * object tags, vehicle display names, and typed component IDs. If no object matches, it falls back
 * to the legacy priority order used by the original player: pig, vehicle, dynamic object, then the
 * first object in the world.
 *
 * @author Paulina Vvedenskaya
 */
final class CameraTargetResolver {

  /**
   * Resolves the camera target for a world.
   *
   * @param world active gameplay world
   * @param cameraFollows level-authored target name from {@code mainCharacter.cameraFollows}
   * @return object for the camera to follow, or null when the world has no objects
   */
  GameObject resolve(World world, String cameraFollows) {
    GameObject cameraFollowsTarget = findCameraFollowsTarget(world, cameraFollows);
    if (cameraFollowsTarget != null) {
      return cameraFollowsTarget;
    }
    return fallbackTarget(world);
  }

  private GameObject findCameraFollowsTarget(World world, String cameraFollows) {
    if (cameraFollows == null || cameraFollows.isBlank()) {
      return null;
    }
    String desired = cameraFollows.trim();
    GameObject byTag = world.getObjectsByTag(desired).stream().findFirst().orElse(null);
    if (byTag != null) {
      return byTag;
    }
    String desiredLower = desired.toLowerCase();
    for (GameObject object : world.getObjects()) {
      if (object instanceof VehicleComponent vc) {
        String displayName = vc.getDisplayName();
        if (displayName != null && displayName.toLowerCase().equals(desiredLower)) {
          return object;
        }
      }
      if (object instanceof TypedComponent typed
          && typed.getComponentType().toLowerCase().equals(desiredLower)) {
        return object;
      }
    }
    return null;
  }

  private GameObject fallbackTarget(World world) {
    GameObject pig = world.getObjectsByTag(DefaultTag.PIGS.value()).stream().findFirst()
        .orElse(null);
    if (pig != null) {
      return pig;
    }

    GameObject vehicle = world.getObjectsByTag(DefaultTag.VEHICLE.value()).stream().findFirst()
        .orElse(null);
    if (vehicle != null) {
      return vehicle;
    }

    GameObject dynamic = world.getObjectsByTag(DefaultTag.DYNAMIC.value()).stream().findFirst()
        .orElse(null);
    if (dynamic != null) {
      return dynamic;
    }

    return world.getObjects().stream().findFirst().orElse(null);
  }
}
