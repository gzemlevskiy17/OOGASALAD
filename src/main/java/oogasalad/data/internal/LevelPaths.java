package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
  private static final ObjectMapper MAPPER = new ObjectMapper();
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
    return gameFileForLevel(levelPath, gameType, Path.of(GAMES_DIR));
  }

  static String gameFileForLevel(String levelPath, String gameType, Path gamesDir) {
    if (gameType == null || gameType.isBlank()) {
      return siblingGameFile(levelPath);
    }
    Path centralCatalog = gamesDir.resolve(gameType).resolve(GAME_FILE_NAME);
    if (hasCatalogContent(centralCatalog)) {
      LOG.debug("Resolved game catalog for '{}' through {}", gameType, centralCatalog);
      return centralCatalog.toString();
    }
    Path generatedCatalog = gamesDir.resolve(gameType).resolve(AI_GENERATION_CATALOG_FILE_NAME);
    if (Files.exists(generatedCatalog)) {
      LOG.debug("Resolved game catalog for '{}' through {}", gameType, generatedCatalog);
      return generatedCatalog.toString();
    }
    return Files.exists(centralCatalog) ? centralCatalog.toString() : siblingGameFile(levelPath);
  }

  private static boolean hasCatalogContent(Path catalogPath) {
    if (!Files.exists(catalogPath)) {
      return false;
    }
    try {
      JsonNode root = MAPPER.readTree(catalogPath.toFile());
      return hasEntries(root, "components")
          || hasEntries(root, "staticComponents")
          || hasEntries(root, "terrainTypes");
    } catch (IOException e) {
      LOG.warn("Could not inspect game catalog {}; falling back to alternate resolution",
          catalogPath, e);
      return false;
    }
  }

  private static boolean hasEntries(JsonNode root, String fieldName) {
    JsonNode field = root.path(fieldName);
    return field.isArray() && field.size() > 0;
  }
}
