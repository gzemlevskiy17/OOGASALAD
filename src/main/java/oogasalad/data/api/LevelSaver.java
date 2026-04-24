package oogasalad.data.api;

import oogasalad.model.engine.api.World;

/** Contract for translating memory to JSON files. */
public interface LevelSaver {
  /**
   * Serializes the current World state into a JSON file.
   * @param fileName the destination path.
   * @param world the simulation state to save.
   */
  void saveLevel(String fileName, World world);
}