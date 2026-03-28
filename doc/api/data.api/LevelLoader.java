package oogasalad.data.api;

import oogasalad.model.engine.api.World;

/** Contract for translating JSON files to memory. */
public interface LevelLoader {
  /**
   * Loads a level from a file and populates the provided World.
   * @param fileName the path to the JSON file.
   * @param world the target container for loaded objects.
   * @param config the configuration context for the level.
   */
  void loadLevel(String fileName, World world, GameConfig config);
}