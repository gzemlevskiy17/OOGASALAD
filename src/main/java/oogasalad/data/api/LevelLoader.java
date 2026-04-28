package oogasalad.data.api;

import oogasalad.model.engine.api.World;

/**
 * Translates a level.json file into runtime World objects + level attributes. The caller must
 * supply the {@link GameConfig} (parsed from the matching game.json) so the loader can resolve
 * mapObject type ids against the catalog.
 *
 * @author Paulina Vvedenskaya
 */
public interface LevelLoader {

  /**
   * Loads a level from a file and populates the provided World and level attributes.
   *
   * @param fileName        path to the level.json file
   * @param game            catalog from the matching game.json
   * @param world           target container for loaded runtime objects
   * @param levelAttributes target container for gameMetadata + mainCharacter values
   */
  void loadLevel(String fileName, GameConfig game, World world, LevelAttributes levelAttributes);

  /**
   * Convenience overload when the caller doesn't need to harvest level attributes.
   *
   * @param fileName path to the level.json file
   * @param game     catalog from the matching game.json
   * @param world    target container for loaded runtime objects
   */
  default void loadLevel(String fileName, GameConfig game, World world) {
    loadLevel(fileName, game, world, new LevelAttributes());
  }

  /**
   * Loads only level metadata/main-character settings without constructing runtime world objects.
   *
   * @param fileName path to the level.json file
   * @return populated level attributes
   */
  LevelAttributes loadAttributes(String fileName);
}
