package oogasalad.data.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Filesystem conventions for the data layer. Keeps the "level.json sits next to its game.json" rule
 * in one place so callers don't reinvent it.
 *
 * @author Paulina Vvedenskaya
 */
public final class LevelPaths {

  private static final Logger LOG = LogManager.getLogger(LevelPaths.class);
  public static final String GAME_FILE_NAME = "game.json";
  public static final String AI_GENERATION_CATALOG_FILE_NAME = "ai-generation-catalog.json";
  private static final String GAMES_DIR = "data/games";

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

  /**
   * Resolves the catalog referenced by a level. Current authored levels store the game id in
   * {@code gameMetadata.gameType}, and game catalogs usually live under
   * {@code data/games/<gameType>/}. Older levels may still sit next to their {@code game.json},
   * so missing metadata or a missing central catalog falls back to the sibling convention.
   *
   * @param levelPath path to a level.json file
   * @param gameType game id from the level's {@code gameMetadata.gameType}
   * @return path to the matching {@code game.json}
   */
  public static String gameFileForLevel(String levelPath, String gameType) {
    if (gameType == null || gameType.isBlank()) {
      return siblingGameFile(levelPath);
    }
    Path generatedCatalog = Path.of(GAMES_DIR, gameType, AI_GENERATION_CATALOG_FILE_NAME);
    if (Files.exists(generatedCatalog)) {
      LOG.debug("Resolved game catalog for '{}' through {}", gameType, generatedCatalog);
      return generatedCatalog.toString();
    }
    Path centralCatalog = Path.of(GAMES_DIR, gameType, GAME_FILE_NAME);
    return Files.exists(centralCatalog) ? centralCatalog.toString() : siblingGameFile(levelPath);
  }
}
