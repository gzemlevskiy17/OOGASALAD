package oogasalad.view.authoring.internal;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.GameCatalogSaver;
import oogasalad.view.core.api.AbstractAppScreen;

/**
 * Game catalog editor screen. Authors can define terrain types, static objects (pickups/hazards),
 * movable components with on-screen buttons, and reusable cheat definitions. The catalog is saved
 * to {@code game.json} and then used to drive the level editor and vehicle builder.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
public class GameAuthoringScreenImpl extends AbstractAppScreen {

  private static final Path ASSETS_DIR = Path.of("data", "assets");
  private static final Path DEFAULT_GAMES_DIR = Path.of("data", "games");
  private static final Path DEFAULT_LEVELS_DIR = Path.of("data", "levels");

  private final Consumer<String> loadLevelHandler;
  private final Consumer<GameConfig> onGameConfigReady;
  private final FileChooserService fileChooserService;
  private final GameCatalogPersistenceController persistenceController;

  private GameCatalogDraft draft;
  private AuthoringAlertService alertService;
  private CheatAuthoringValidator cheatValidator;
  private SpriteImportService spriteImportService;
  private TextField gameIdField;
  private TextField mainCharacterIdField;
  private TerrainCatalogTab terrainTab;
  private StaticObjectCatalogTab staticObjectTab;
  private ComponentCatalogTab componentTab;
  private CheatCatalogTab cheatTab;

  public GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady) {
    this(onOutcome, loadLevelHandler, onGameConfigReady, new GameCatalogSaver(),
        DEFAULT_GAMES_DIR, DEFAULT_LEVELS_DIR, new FileChooserService());
  }

  GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady, GameCatalogSaver catalogSaver, Path gamesDir,
      Path levelsDir, FileChooserService fileChooserService) {
    super(onOutcome);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.onGameConfigReady = onGameConfigReady != null ? onGameConfigReady : cfg -> {
    };
    this.fileChooserService = Objects.requireNonNull(fileChooserService);
    persistenceController = new GameCatalogPersistenceController(draft(),
        Objects.requireNonNull(catalogSaver), Objects.requireNonNull(gamesDir),
        Objects.requireNonNull(levelsDir), cheatValidator(), alertService());
  }

  public void loadCatalogFromPath(String path) {
    persistenceController.loadCatalog(path).ifPresent(config -> {
      Optional.ofNullable(gameIdField).ifPresent(field -> field.setText(config.gameId()));
      Optional.ofNullable(mainCharacterIdField)
          .ifPresent(field -> field.setText(config.mainCharacterId()));
      refreshCatalogTabs();
    });
  }

  public void resetForNewCatalog() {
    draft().clear();
    Optional.ofNullable(gameIdField).ifPresent(TextField::clear);
    Optional.ofNullable(mainCharacterIdField).ifPresent(TextField::clear);
    refreshCatalogTabs();
  }

  @Override
  protected Parent createRoot() {
    GameAuthoringLayoutFactory.Parts parts = new GameAuthoringLayoutFactory(
        draft(), spriteImportService(), alertService(), cheatValidator(),
        this::mainCharacterId, this::setMainCharacterId, this::handleLoadCatalog,
        this::handleLoadLevel, this::handleSaveCatalog, this::navigateToLevelEditor,
        () -> onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY))
        .build();
    gameIdField = parts.gameIdField();
    mainCharacterIdField = parts.mainCharacterIdField();
    terrainTab = parts.terrainTab();
    staticObjectTab = parts.staticObjectTab();
    componentTab = parts.componentTab();
    cheatTab = parts.cheatTab();
    return parts.root();
  }

  private void handleSaveCatalog() {
    persistenceController.saveWithUserMessage(gameId(), mainCharacterId());
  }

  private void handleLoadCatalog(Button source) {
    fileChooserService.chooseLoadFile(source.getScene().getWindow())
        .ifPresent(file -> loadCatalogFromPath(file.getAbsolutePath()));
  }

  private void handleLoadLevel(Button source) {
    fileChooserService.chooseLoadFile(source.getScene().getWindow())
        .ifPresent(file -> {
          loadLevelHandler.accept(file.getAbsolutePath());
          navigateToLevelEditor();
        });
  }

  private void navigateToLevelEditor() {
    buildCurrentConfig().ifPresent(config -> {
      onGameConfigReady.accept(config);
      onOutcome.accept(Outcome.OPEN_AUTHORING);
    });
  }

  private Optional<GameConfig> buildCurrentConfig() {
    try {
      return Optional.of(
          persistenceController.saveAndLoadCurrentConfig(gameId(), mainCharacterId()));
    } catch (IllegalStateException e) {
      return Optional.empty();
    }
  }

  private void refreshCatalogTabs() {
    Optional.ofNullable(terrainTab).ifPresent(TerrainCatalogTab::refresh);
    Optional.ofNullable(staticObjectTab).ifPresent(StaticObjectCatalogTab::refresh);
    Optional.ofNullable(componentTab).ifPresent(ComponentCatalogTab::refresh);
    Optional.ofNullable(cheatTab).ifPresent(CheatCatalogTab::refresh);
  }

  private String gameId() {
    return Optional.ofNullable(gameIdField)
        .map(TextField::getText)
        .orElse("")
        .strip();
  }

  private String mainCharacterId() {
    return Optional.ofNullable(mainCharacterIdField)
        .map(TextField::getText)
        .orElse("")
        .strip();
  }

  private void setMainCharacterId(String id) {
    Optional.ofNullable(mainCharacterIdField).ifPresent(field -> field.setText(id));
  }

  private GameCatalogDraft draft() {
    if (draft == null) {
      draft = new GameCatalogDraft();
    }
    return draft;
  }

  private AuthoringAlertService alertService() {
    if (alertService == null) {
      alertService = new AuthoringAlertService();
    }
    return alertService;
  }

  private CheatAuthoringValidator cheatValidator() {
    if (cheatValidator == null) {
      cheatValidator = new CheatAuthoringValidator();
    }
    return cheatValidator;
  }

  private SpriteImportService spriteImportService() {
    if (spriteImportService == null) {
      spriteImportService = new SpriteImportService(ASSETS_DIR, alertService());
    }
    return spriteImportService;
  }
}
