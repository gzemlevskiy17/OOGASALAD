package oogasalad.data.api;

import oogasalad.model.engine.api.World;

/** Contract for translating JSON files to memory. */
public interface LevelLoader {
  /**
   * Loads a level from a file and populates the provided World and level attributes.
   *
   * @param fileName the path to the JSON file
   * @param world the target container for loaded objects
   * @param levelAttributes the target container for loaded level metadata
   * @param config the configuration context for the level
   * @author Emilio Garcia
   */
  void loadLevel(String fileName, World world, LevelAttributes levelAttributes, GameConfig config);

  /**
   * Loads a level from a file when no attribute target is needed by the caller.
   *
   * @param fileName the path to the JSON file
   * @param world the target container for loaded objects
   * @param config the configuration context for the level
   * @author Emilio Garcia
   */
  default void loadLevel(String fileName, World world, GameConfig config) {
    loadLevel(fileName, world, new LevelAttributes(), config);
  }
}
