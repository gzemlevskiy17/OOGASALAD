package oogasalad.data.api;

import oogasalad.data.internal.LevelAttributes;
import oogasalad.model.engine.api.World;

/** Contract for translating memory to JSON files. */
public interface LevelSaver {
  /**
   * Serializes the current World state and level attributes into a JSON file.
   *
   * @param fileName the destination path
   * @param world the simulation state to save
   * @param levelAttributes metadata to save alongside terrain and objects
   * @author Emilio Garcia
   */
  void saveLevel(String fileName, World world, LevelAttributes levelAttributes);

  /**
   * Serializes only the current World state when no attribute source is provided by the caller.
   *
   * @param fileName the destination path
   * @param world the simulation state to save
   * @author Emilio Garcia
   */
  default void saveLevel(String fileName, World world) {
    saveLevel(fileName, world, new LevelAttributes());
  }
}
