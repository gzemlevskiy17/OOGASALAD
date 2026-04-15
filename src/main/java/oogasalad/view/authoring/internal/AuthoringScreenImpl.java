package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.controller.authoring.internal.ToolSelectorImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelSaveException;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelSaverImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Layout shell for the authoring environment.
 */
public class AuthoringScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private enum AuthoringMode {
    LEVEL,
    VEHICLE
  }

  private enum LevelPaletteMode {
    TERRAIN,
    PIGS,
    LEVEL_ITEMS,
    CUSTOM,
    SETTINGS
  }

  private AuthoringControllerImpl controller;
  private StackPane canvasHost;
  private ComponentPallete vehiclePalette;
  private ComponentPaletteImpl terrainPalette;
  private ComponentPaletteImpl pigsPalette;
  private ComponentPaletteImpl levelItemsPalette;
  private ComponentPaletteImpl customPalette;
  private AuthoringAlertService alertService;
  private FileChooserService fileChooserService;
  private HBox paletteModeRow;
  private StackPane bottomContentHost;
  private Button levelModeButton;
  private Button vehicleModeButton;
  private Button terrainPaletteButton;
  private Button pigsPaletteButton;
  private Button levelItemsPaletteButton;
  private Button customPaletteButton;
  private Button settingsPaletteButton;
  private LevelAttributesPanel levelAttributesPanel;
  private LevelAttributes levelAttributes;
  private TerrainPropertiesPanel terrainPropertiesPanel;
  private World world;
  private AuthoringCanvasImpl canvas;

  private AuthoringMode currentMode;
  private LevelPaletteMode currentLevelPaletteMode;

  public AuthoringScreenImpl(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  @Override
  protected Parent createRoot() {
    initializeAuthoringState();

    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-screen.css").toExternalForm());
    layout.getStyleClass().add("screen-root");
    layout.setPadding(new Insets(d("rootPadding")));

    VBox topSections = new VBox(
        d("outerSectionSpacing"),
        buildTopBar(),
        buildScreenModeRow());
    topSections.getStyleClass().add("authoring-top-sections");

    VBox bottomSections = new VBox(
        d("outerSectionSpacing"),
        paletteModeRow,
        buildBottomRegion());
    bottomSections.getStyleClass().add("authoring-bottom-sections");

    canvasHost.getStyleClass().add("authoring-canvas-host");
    VBox.setVgrow(canvasHost, Priority.ALWAYS);
    layout.setTop(topSections);
    layout.setCenter(canvasHost);
    layout.setBottom(bottomSections);
    layout.setRight(terrainPropertiesPanel.getVisualRoot());

    wireModeHandlers();
    refreshDisplayedMode();
    return layout;
  }

  /**
   * Clears all placed objects and resets the canvas for a fresh level.
   */
  public void resetForNewLevel() {
    world.clear();
    terrainPropertiesPanel.hide();
    canvas.refreshGrid();
  }

  public void loadLevelFromPath(String path) {
    try {
      controller.loadLevel(path);
    } catch (LevelLoadException e) {
      alertService.showError(PROPS.getString("loadErrorTitle"), e.getMessage());
    }
  }

  private void initializeAuthoringState() {
    world = new WorldImpl();
    levelAttributes = new LevelAttributes();
    levelAttributesPanel = new LevelAttributesPanel(levelAttributes);
    terrainPropertiesPanel = new TerrainPropertiesPanel();
    canvas = new AuthoringCanvasImpl(world);
    canvas.setOnObjectSelected(terrainPropertiesPanel::show);
    controller = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(), new LevelSaverImpl(),
        levelAttributes, new ToolSelectorImpl(), canvas);
    alertService = new AuthoringAlertService();
    fileChooserService = new FileChooserService();
    controller.setPlacementErrorHandler(
        message -> alertService.showWarning(s("placementWarningTitle"), message));
    controller.initializeCanvasHandlers();

    ScrollPane canvasScrollPane = new ScrollPane(canvas.getVisualRoot());
    canvasScrollPane.setFitToWidth(false);
    canvasScrollPane.setFitToHeight(false);
    canvasScrollPane.setPannable(true);
    canvasScrollPane.getStyleClass().add("authoring-canvas-scroll");
    canvasHost = new StackPane(canvasScrollPane);

    vehiclePalette = new VehicleComponentScrollPalette();
    terrainPalette = new ComponentPaletteImpl();
    pigsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.PigsPalette");
    levelItemsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.LevelItemsPalette");
    customPalette = new ComponentPaletteImpl("oogasalad.view.authoring.CustomPalette");
    subscribePalette(terrainPalette);
    subscribePalette(pigsPalette);
    subscribePalette(levelItemsPalette);
    subscribePalette(customPalette);

    terrainPaletteButton = createTabButton(s("terrainButtonText"));
    pigsPaletteButton = createTabButton(s("pigsButtonText"));
    levelItemsPaletteButton = createTabButton(s("levelItemsButtonText"));
    customPaletteButton = createTabButton(s("customButtonText"));
    settingsPaletteButton = createTabButton(s("settingsButtonText"));
    paletteModeRow = buildPaletteModeRow();
    bottomContentHost = new StackPane();
    levelModeButton = createTabButton(s("levelButtonText"));
    vehicleModeButton = createTabButton(s("vehicleButtonText"));
    currentMode = AuthoringMode.LEVEL;
    currentLevelPaletteMode = LevelPaletteMode.TERRAIN;
  }

  private void subscribePalette(ComponentPaletteImpl palette) {
    palette.subscribe(canvas::setSelectedPlaceable);
    palette.subscribe(controller::setSelectedPlaceable);
  }

  private void wireModeHandlers() {
    levelModeButton.setOnAction(event -> {
      currentMode = AuthoringMode.LEVEL;
      refreshDisplayedMode();
    });
    vehicleModeButton.setOnAction(event -> {
      currentMode = AuthoringMode.VEHICLE;
      refreshDisplayedMode();
    });
    terrainPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.TERRAIN;
      refreshDisplayedMode();
    });
    pigsPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.PIGS;
      refreshDisplayedMode();
    });
    levelItemsPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.LEVEL_ITEMS;
      refreshDisplayedMode();
    });
    customPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.CUSTOM;
      refreshDisplayedMode();
    });
    settingsPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.SETTINGS;
      refreshDisplayedMode();
    });
  }

  private HBox buildTopBar() {
    Button backButton = createSizedButton(
        s("backButtonText"),
        d("backButtonWidth"),
        d("backButtonHeight"));
    backButton.getStyleClass().add("authoring-back-button");
    backButton.setOnAction(event -> {
      if (!world.getObjects().isEmpty()) {
        boolean confirmed = alertService.showConfirmation(
            s("leaveWarningTitle"),
            s("leaveWarningMessage"));
        if (!confirmed) {
          return;
        }
      }
      onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY);
    });

    double actionWidth = d("topActionButtonWidth");
    double actionHeight = d("topActionButtonHeight");

    Button drawButton = createSizedButton(s("drawButtonText"), actionWidth, actionHeight);
    Button eraseButton = createSizedButton(s("eraseButtonText"), actionWidth, actionHeight);
    drawButton.getStyleClass().add("authoring-action-button");
    eraseButton.getStyleClass().add("authoring-action-button");
    drawButton.setOnAction(event -> controller.setActiveTool(ToolType.DRAW));
    eraseButton.setOnAction(event -> controller.setActiveTool(ToolType.ERASE));

    Button saveButton = createSizedButton(s("saveButtonText"), actionWidth, actionHeight);
    Button runButton = createSizedButton(s("runButtonText"), actionWidth, actionHeight);
    Button loadButton = createSizedButton(s("loadButtonText"), actionWidth, actionHeight);
    saveButton.getStyleClass().add("authoring-action-button");
    runButton.getStyleClass().add("authoring-action-button");
    loadButton.getStyleClass().add("authoring-action-button");

    saveButton.setOnAction(event -> handleSave(saveButton.getScene().getWindow()));
    loadButton.setOnAction(event -> handleLoad(loadButton.getScene().getWindow()));

    HBox toolButtons = new HBox(d("topBarSpacing"), drawButton, eraseButton);
    toolButtons.setAlignment(Pos.CENTER_LEFT);

    HBox rightButtons = new HBox(
        d("topBarSpacing"), saveButton, runButton, loadButton);
    rightButtons.setAlignment(Pos.CENTER_RIGHT);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(backButton, toolButtons, spacer, rightButtons);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.getStyleClass().add("authoring-top-bar");
    return topBar;
  }

  private HBox buildScreenModeRow() {
    HBox row = new HBox(levelModeButton, vehicleModeButton);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("authoring-screen-mode-row");
    return row;
  }

  private HBox buildPaletteModeRow() {
    HBox row = new HBox(
        terrainPaletteButton,
        pigsPaletteButton,
        levelItemsPaletteButton,
        customPaletteButton,
        settingsPaletteButton);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("authoring-palette-mode-row");
    return row;
  }

  private StackPane buildBottomRegion() {
    StackPane region = bottomContentHost;
    double bottomHeight = d("bottomRegionHeight");
    region.setPrefHeight(bottomHeight);
    region.setMinHeight(bottomHeight);
    region.setMinWidth(d("bottomRegionMinWidth"));
    region.setAlignment(Pos.TOP_LEFT);
    region.getStyleClass().add("authoring-bottom-region");
    return region;
  }

  private void refreshDisplayedMode() {
    boolean isLevelMode = currentMode == AuthoringMode.LEVEL;
    paletteModeRow.setVisible(isLevelMode);
    paletteModeRow.setManaged(isLevelMode);

    bottomContentHost.getChildren().clear();
    Node paletteNode;
    if (currentMode == AuthoringMode.VEHICLE) {
      paletteNode = vehiclePalette.getVisualRoot();
    } else {
      paletteNode = switch (currentLevelPaletteMode) {
        case TERRAIN -> terrainPalette.getVisualRoot();
        case PIGS -> pigsPalette.getVisualRoot();
        case LEVEL_ITEMS -> levelItemsPalette.getVisualRoot();
        case CUSTOM -> customPalette.getVisualRoot();
        case SETTINGS -> levelAttributesPanel.getVisualRoot();
      };
    }
    StackPane.setAlignment(paletteNode, Pos.TOP_LEFT);
    bottomContentHost.getChildren().add(paletteNode);
  }

  private void handleSave(Window owner) {
    fileChooserService.chooseSaveFile(owner).ifPresent(file -> {
      try {
        controller.saveLevel(file.getAbsolutePath());
      } catch (LevelSaveException e) {
        alertService.showError(PROPS.getString("saveErrorTitle"), e.getMessage());
      }
    });
  }

  private void handleLoad(Window owner) {
    fileChooserService.chooseLoadFile(owner)
        .ifPresent(file -> loadLevelFromPath(file.getAbsolutePath()));
  }

  private Button createTabButton(String text) {
    Button button = createSizedButton(
        text,
        d("smallTabButtonWidth"),
        d("smallTabButtonHeight"));
    button.getStyleClass().add("authoring-palette-button");
    return button;
  }

  private Button createSizedButton(String text, double width, double height) {
    Button button = new Button(text);
    button.setPrefWidth(width);
    button.setPrefHeight(height);
    button.getStyleClass().add("authoring-button");
    return button;
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
