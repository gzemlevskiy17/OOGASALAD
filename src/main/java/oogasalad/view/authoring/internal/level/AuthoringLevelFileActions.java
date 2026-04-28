package oogasalad.view.authoring.internal.level;

import java.io.File;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.stage.Window;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelSaveException;
import oogasalad.view.authoring.internal.common.AuthoringAlertService;
import oogasalad.view.authoring.internal.common.FileChooserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles authoring level save, load, and run-preview file actions.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
final class AuthoringLevelFileActions {

  private static final Logger LOG = LogManager.getLogger(AuthoringLevelFileActions.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");
  private static final String RUN_PREVIEW_PATH = "data/_authoring_preview.json";

  private final AuthoringControllerImpl controller;
  private final AuthoringAlertService alertService;
  private final FileChooserService fileChooserService;
  private final Supplier<GameConfig> gameConfigSupplier;
  private final Consumer<String> previewSavedHandler;

  AuthoringLevelFileActions(AuthoringControllerImpl controller,
      AuthoringAlertService alertService, FileChooserService fileChooserService,
      Supplier<GameConfig> gameConfigSupplier, Consumer<String> previewSavedHandler) {
    this.controller = controller;
    this.alertService = alertService;
    this.fileChooserService = fileChooserService;
    this.gameConfigSupplier = gameConfigSupplier;
    this.previewSavedHandler = previewSavedHandler;
  }

  void save(Window owner) {
    File initialDir = gameConfigSupplier.get() != null
        ? new File("data/levels/" + gameConfigSupplier.get().gameId())
        : null;
    fileChooserService.chooseSaveFile(owner, initialDir).ifPresent(file -> {
      try {
        controller.saveLevel(file.getAbsolutePath());
      } catch (LevelSaveException e) {
        LOG.error("Level save rejected for '{}': {}", file.getAbsolutePath(), e.getMessage(), e);
        alertService.showError(PROPS.getString("saveErrorTitle"), e.getMessage());
      }
    });
  }

  void load(Window owner, Consumer<String> loadLevelHandler) {
    fileChooserService.chooseLoadFile(owner)
        .ifPresent(file -> loadLevelHandler.accept(file.getAbsolutePath()));
  }

  void run() {
    try {
      controller.saveLevel(RUN_PREVIEW_PATH);
    } catch (LevelSaveException e) {
      LOG.error("Run-preview save rejected for '{}': {}", RUN_PREVIEW_PATH, e.getMessage(), e);
      alertService.showError(PROPS.getString("saveErrorTitle"), e.getMessage());
      return;
    }
    previewSavedHandler.accept(RUN_PREVIEW_PATH);
  }
}
