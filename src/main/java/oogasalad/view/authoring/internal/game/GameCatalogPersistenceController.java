package oogasalad.view.authoring.internal.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.GameCatalogSaver;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.view.authoring.internal.common.AuthoringAlertService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates game catalog persistence for the game authoring screen.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class GameCatalogPersistenceController {

  private static final Logger LOG =
      LogManager.getLogger(GameCatalogPersistenceController.class);
  private static final String GAME_FILE_NAME = "game.json";

  private final GameCatalogDraft draft;
  private final GameCatalogSaver catalogSaver;
  private final Path gamesDir;
  private final Path levelsDir;
  private final CheatAuthoringValidator cheatValidator;
  private final AuthoringAlertService alertService;

  GameCatalogPersistenceController(GameCatalogDraft draft, GameCatalogSaver catalogSaver,
      Path gamesDir, Path levelsDir, CheatAuthoringValidator cheatValidator,
      AuthoringAlertService alertService) {
    this.draft = Objects.requireNonNull(draft);
    this.catalogSaver = Objects.requireNonNull(catalogSaver);
    this.gamesDir = Objects.requireNonNull(gamesDir);
    this.levelsDir = Objects.requireNonNull(levelsDir);
    this.cheatValidator = Objects.requireNonNull(cheatValidator);
    this.alertService = Objects.requireNonNull(alertService);
  }

  Optional<GameConfig> loadCatalog(String path) {
    try {
      GameConfig config = new GameConfigImpl(path);
      draft.loadFrom(config, path);
      return Optional.of(config);
    } catch (Exception e) {
      LOG.warn("Could not load game catalog from {}", path, e);
      alertService.showError("Load Error", e.getMessage());
      return Optional.empty();
    }
  }

  void saveWithUserMessage(String gameId, String mainCharacterId) {
    try {
      Path catalogPath = saveCatalogForGameId(gameId, mainCharacterId);
      draft.setSaveFilePath(catalogPath.toString());
      alertService.showInfo("Saved", "Game catalog saved to " + draft.saveFilePath());
    } catch (IOException | RuntimeException e) {
      LOG.warn("Could not save game catalog", e);
      alertService.showError("Save Error", e.getMessage());
    }
  }

  GameConfig saveAndLoadCurrentConfig(String gameId, String mainCharacterId) {
    try {
      Path catalogPath = saveCatalogForGameId(gameId, mainCharacterId);
      draft.setSaveFilePath(catalogPath.toString());
      return new GameConfigImpl(draft.saveFilePath());
    } catch (IOException | RuntimeException e) {
      LOG.warn("Could not prepare current game config", e);
      alertService.showError("Save Error", e.getMessage());
      return GameConfigImpl.empty();
    }
  }

  private Path saveCatalogForGameId(String gameId, String mainCharacterId) throws IOException {
    if (gameId.isBlank()) {
      throw new IllegalArgumentException("Game ID is required before saving a game catalog.");
    }
    Path catalogPath = catalogPathFor(gameId);
    Files.createDirectories(catalogPath.getParent());
    Files.createDirectories(levelsDir.resolve(gameId));
    validateMainCharacterId(mainCharacterId);
    cheatValidator.validateCheats(draft.cheats(), draft.staticComponents());
    catalogSaver.save(catalogPath.toString(), gameId, mainCharacterId,
        draft.components(), draft.staticComponents(), draft.terrainTypes(), draft.cheats());
    return catalogPath;
  }

  private void validateMainCharacterId(String mainCharacterId) {
    if (mainCharacterId.isBlank()) {
      throw new IllegalArgumentException(
          "Main Character is required before saving a game catalog.");
    }
    boolean exists = draft.components().stream()
        .anyMatch(component -> mainCharacterId.equals(component.id()));
    if (!exists) {
      throw new IllegalArgumentException(
          "Main Character must be one of the vehicle components.");
    }
  }

  private Path catalogPathFor(String gameId) {
    return gamesDir.resolve(gameId).resolve(GAME_FILE_NAME);
  }
}
