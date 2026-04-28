package oogasalad.model.player.internal;

import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.api.World;

/**
 * Finds the runtime objects that represent the game-defined main character.
 *
 * <p>The game catalog names the main character by component id, while older runtime code often
 * tags main-character parts as {@code pigs}. This lookup supports both forms so catalog-defined
 * main characters work without losing compatibility with existing Bad Piggies levels.
 *
 * @author Paulina Vvedenskaya
 */
final class MainCharacterLookup {

  private MainCharacterLookup() {
  }

  /**
   * Finds all objects matching the supplied main character id.
   *
   * @param world gameplay world to search
   * @param mainCharacterId component id from game.json, or blank to use the legacy pig tag
   * @return matching runtime objects
   */
  static List<GameObject> find(World world, String mainCharacterId) {
    if (mainCharacterId == null || mainCharacterId.isBlank()) {
      return world.getObjectsByTag(DefaultTag.PIGS.value());
    }
    List<GameObject> byTag = world.getObjectsByTag(mainCharacterId);
    if (!byTag.isEmpty()) {
      return byTag;
    }
    List<GameObject> byType = world.getObjects().stream()
        .filter(object -> object instanceof TypedComponent typed
            && mainCharacterId.equals(typed.getComponentType()))
        .toList();
    if (!byType.isEmpty()) {
      return byType;
    }
    // fall back to any pig — handles cases where a different pig variant was placed
    return world.getObjectsByTag(DefaultTag.PIGS.value());
  }
}
