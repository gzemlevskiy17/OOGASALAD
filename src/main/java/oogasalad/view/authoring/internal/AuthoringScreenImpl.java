package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoadException;
import oogasalad.view.core.internal.AbstractAppScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates the authoring environment lifecycle, catalog state, and navigation callbacks.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
public class AuthoringScreenImpl extends AbstractAppScreen {

  private static final Logger LOG = LogManager.getLogger(AuthoringScreenImpl.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private Consumer<String> onRunLevel = path -> {
  };
  private GameConfig gameConfig;
  private AuthoringWorkspaceFactory.Workspace workspace;
  private AuthoringPaletteCatalogAdapter paletteCatalogAdapter;
  private AuthoringModeController modeController;
  private AuthoringLevelFileActions fileActions;
  private AuthoringAlertService alertService;
  private FileChooserService fileChooserService;
  private boolean preserveLoadedLevelOnNextEnter;

  public AuthoringScreenImpl(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  public AuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> onRunLevel) {
    super(onOutcome);
    this.onRunLevel = onRunLevel == null ? path -> {
    } : onRunLevel;
  }

  @Override
  protected Parent createRoot() {
    initializeAuthoringState();
    AuthoringScreenLayoutFactory.Parts layoutParts = buildLayout();
    modeController = new AuthoringModeController(layoutParts, paletteCatalogAdapter,
        workspace.levelAttributesPanel());
    paletteCatalogAdapter.setSelectionSummaryHandler(modeController::updateSelectedItemsSummary);
    modeController.wireHandlers(workspace.controller());
    modeController.refreshDisplayedMode();
    return layoutParts.root();
  }

  @Override
  public void onEnter() {
    if (preserveLoadedLevelOnNextEnter) {
      preserveLoadedLevelOnNextEnter = false;
    } else {
      resetForNewLevel();
    }
    workspace.canvas().setZoom(1.0);
  }

  public void resetForNewLevel() {
    workspace.controller().clearLevel();
    workspace.terrainPropertiesPanel().hide();
  }

  public void loadLevelFromPath(String path) {
    try {
      workspace.controller().loadLevel(path);
      paletteCatalogAdapter.syncVehiclePaletteSelection();
      preserveLoadedLevelOnNextEnter = true;
    } catch (LevelLoadException e) {
      LOG.error("Level load rejected for '{}': {}", path, e.getMessage(), e);
      alertService.showError(PROPS.getString("loadErrorTitle"), e.getMessage());
    }
  }

  public void setGameConfig(GameConfig config) {
    gameConfig = config;
    if (paletteCatalogAdapter != null) {
      paletteCatalogAdapter.setGameConfig(config);
    }
  }

  private void initializeAuthoringState() {
    workspace = new AuthoringWorkspaceFactory().create();
    alertService = new AuthoringAlertService();
    fileChooserService = new FileChooserService();
    paletteCatalogAdapter = new AuthoringPaletteCatalogAdapter(workspace.levelAttributes(),
        workspace.canvas(), workspace.controller());
    fileActions = new AuthoringLevelFileActions(workspace.controller(), alertService,
        fileChooserService, () -> gameConfig, this::completeRunPreview);
  }

  private AuthoringScreenLayoutFactory.Parts buildLayout() {
    return new AuthoringScreenLayoutFactory(
        workspace.canvasHost(),
        workspace.terrainPropertiesPanel().getVisualRoot(),
        this::handleBack,
        workspace.controller()::commitSpline,
        this::handleUndo,
        this::handleRedo,
        this::handleClear,
        this::zoomIn,
        this::zoomOut,
        workspace::fitCanvasToViewport,
        owner -> fileActions.save(owner),
        fileActions::run,
        owner -> fileActions.load(owner, this::loadLevelFromPath))
        .build();
  }

  private void handleBack() {
    if (!workspace.world().getObjects().isEmpty()) {
      boolean confirmed = alertService.showConfirmation(s("leaveWarningTitle"),
          s("leaveWarningMessage"));
      if (!confirmed) {
        return;
      }
    }
    onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY);
  }

  private void handleUndo() {
    workspace.controller().undo();
    workspace.terrainPropertiesPanel().hide();
  }

  private void handleRedo() {
    workspace.controller().redo();
    workspace.terrainPropertiesPanel().hide();
  }

  private void handleClear() {
    if (!workspace.world().getObjects().isEmpty()) {
      boolean confirmed = alertService.showConfirmation(
          s("clearWarningTitle"), s("clearWarningMessage"));
      if (!confirmed) {
        return;
      }
    }
    resetForNewLevel();
  }

  private void zoomIn() {
    workspace.canvas().setZoom(workspace.canvas().getZoom() * d("zoomStep"));
  }

  private void zoomOut() {
    workspace.canvas().setZoom(workspace.canvas().getZoom() / d("zoomStep"));
  }

  private void completeRunPreview(String previewPath) {
    onRunLevel.accept(previewPath);
    onOutcome.accept(Outcome.OPEN_VEHICLE_BUILDER);
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
