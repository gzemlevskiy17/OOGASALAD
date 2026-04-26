package oogasalad.data.internal;

import java.nio.file.Path;

/**
 * Filesystem conventions for the data layer. Keeps the "level.json sits next to its game.json" rule
 * in one place so callers don't reinvent it.
 *
 * @author Paulina Vvedenskaya
 */
public final class LevelPaths {

  public static final String GAME_FILE_NAME = "game.json";

  private LevelPaths() {
  }

  /**
   * @param levelPath path to a level.json file
   * @return the path to the {@code game.json} in the same directory; falls back to
   * {@code "game.json"} when the level file has no parent directory
   */
  public static String siblingGameFile(String levelPath) {
    Path parent = Path.of(levelPath).getParent();
    if (parent == null) {
      return GAME_FILE_NAME;
    }
    return parent.resolve(GAME_FILE_NAME).toString();
  }
}
