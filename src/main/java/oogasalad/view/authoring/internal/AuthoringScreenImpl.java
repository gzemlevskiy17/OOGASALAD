package oogasalad.view.authoring.internal;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelSaveException;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelSaverImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Layout shell for the authoring environment.
 *
 * @author Emilio Garcia
 */
public class AuthoringScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");
  private static final String DEFAULT_TERRAIN_COLOR = "#8B4513";
  private static final String DEFAULT_STATIC_COLOR = "#F5C542";

  /**
   * Preview file used when the designer clicks Run. We write the authored world here, point the
   * gameplay session at this path, and then open the Vehicle Builder screen — identical to the
   * player flow where the selected level path drives a build → play sequence.
   */
  private static final String RUN_PREVIEW_PATH = "data/_authoring_preview.json";

  private enum AuthoringMode {
    LEVEL,
    VEHICLE
  }

  private enum LevelPaletteMode {
    TERRAIN,
    PIGS,
    LEVEL_ITEMS,
    SETTINGS
  }

  private Consumer<String> onRunLevel = path -> {};
  private GameConfig gameConfig;
  private AuthoringControllerImpl controller;
  private StackPane canvasHost;
  private VehicleComponentScrollPalette vehiclePalette;
  private ComponentPaletteImpl terrainPalette;
  private ComponentPaletteImpl pigsPalette;
  private ComponentPaletteImpl levelItemsPalette;
  private AuthoringAlertService alertService;
  private FileChooserService fileChooserService;
  private HBox paletteModeRow;
  private StackPane bottomContentHost;
  private Button levelModeButton;
  private Button vehicleModeButton;
  private Button terrainPaletteButton;
  private Button pigsPaletteButton;
  private Button levelItemsPaletteButton;
  private Button settingsPaletteButton;
  private HBox levelPaletteTabs;
  private Label selectedItemsLabel;
  private LevelAttributesPanel levelAttributesPanel;
  private LevelAttributes levelAttributes;
  private TerrainPropertiesPanel terrainPropertiesPanel;
  private World world;
  private AuthoringCanvasImpl canvas;
  private ScrollPane canvasScrollPane;
  private List<Button> toolButtons;
  private AuthoringMode currentMode;
  private LevelPaletteMode currentLevelPaletteMode;
  private boolean preserveLoadedLevelOnNextEnter;

  /**
   * Creates the authoring screen.
   *
   * @param onOutcome callback used for navigation outcomes
   */
  public AuthoringScreenImpl(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  /**
   * @param onOutcome  routes screen transitions
   * @param onRunLevel receives the saved preview path when Run is clicked
   */
  public AuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> onRunLevel) {
    super(onOutcome);
    this.onRunLevel = onRunLevel == null ? path -> {} : onRunLevel;
  }

  @Override
  protected Parent createRoot() {
    initializeAuthoringState();

    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-screen.css").toExternalForm());
    layout.getStyleClass().add("screen-root");
    layout.setPadding(new Insets(d("rootPadding")));

    VBox topSections = new VBox(d("outerSectionSpacing"), buildTopBar(), buildScreenModeRow());
    topSections.getStyleClass().add("authoring-top-sections");

    VBox bottomSections = new VBox(d("outerSectionSpacing"), paletteModeRow, buildBottomRegion());
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

  @Override
  public void onEnter() {
    if (preserveLoadedLevelOnNextEnter) {
      preserveLoadedLevelOnNextEnter = false;
    } else {
      resetForNewLevel();
    }
    canvas.setZoom(1.0);
  }

  /**
   * Clears the current level and resets the visible editor state.
   *
   * @author Emilio Garcia
   */
  public void resetForNewLevel() {
    controller.clearLevel();
    terrainPropertiesPanel.hide();
  }

  /**
   * Loads a saved level into the authoring environment.
   *
   * @param path path to the level file
   * @author Emilio Garcia
   */
  public void loadLevelFromPath(String path) {
    try {
      controller.loadLevel(path);
      preserveLoadedLevelOnNextEnter = true;
    } catch (LevelLoadException e) {
      alertService.showError(PROPS.getString("loadErrorTitle"), e.getMessage());
    }
  }

  /**
   * Sets the active game catalog so the level editor knows which game it is authoring for. Call
   * this before navigating to OPEN_AUTHORING.
   */
  public void setGameConfig(GameConfig config) {
    this.gameConfig = config;
    if (config != null && levelAttributes != null) {
      levelAttributes.setGameType(config.gameId());
    }
    refreshCatalogPalettes();
  }

  private void initializeAuthoringState() {
    world = new WorldImpl();
    levelAttributes = new LevelAttributes();
    levelAttributesPanel = new LevelAttributesPanel(levelAttributes);
    terrainPropertiesPanel = new TerrainPropertiesPanel();
    canvas = new AuthoringCanvasImpl(world);
    canvas.setOnObjectSelected(terrainPropertiesPanel::show);
    double cellSize = canvas.getCellSize();
    controller = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(), new LevelSaverImpl(cellSize), new GameLoaderImpl(),
        levelAttributes, new ToolSelectorImpl(), canvas);
    alertService = new AuthoringAlertService();
    fileChooserService = new FileChooserService();
    selectedItemsLabel = buildSelectedItemsLabel();
    controller.initializeCanvasHandlers();

    canvasScrollPane = new ScrollPane(canvas.getVisualRoot());
    canvasScrollPane.setFitToWidth(false);
    canvasScrollPane.setFitToHeight(false);
    canvasScrollPane.setPannable(false);
    canvasScrollPane.getStyleClass().add("authoring-canvas-scroll");
    wireEdgePan(canvasScrollPane);
    wireZoomScroll(canvasScrollPane);
    canvasHost = new StackPane(canvasScrollPane);

    vehiclePalette = new VehicleComponentScrollPalette();
    vehiclePalette.setSelectedComponentsHandler(
        ids -> levelAttributes.setElementsAllowed(ids));
    vehiclePalette.setSelectionSummaryHandler(this::updateSelectedItemsSummary);
    levelAttributes.setMaxSelectable(vehiclePalette.getMaxSelectableItems());

    terrainPalette = new ComponentPaletteImpl();
    pigsPalette = new ComponentPaletteImpl();
    levelItemsPalette = new ComponentPaletteImpl();
    subscribePalette(terrainPalette);
    terrainPalette.subscribe(controller::setSplineTerrainStyle);
    subscribePalette(pigsPalette);
    subscribePalette(levelItemsPalette);

    terrainPaletteButton = createTabButton(s("terrainButtonText"));
    pigsPaletteButton = createTabButton(s("pigsButtonText"));
    levelItemsPaletteButton = createTabButton(s("levelItemsButtonText"));
    settingsPaletteButton = createTabButton(s("settingsButtonText"));
    levelPaletteTabs = buildLevelPaletteTabs();
    paletteModeRow = buildPaletteModeRow();
    bottomContentHost = new StackPane();
    levelModeButton = createTabButton(s("levelButtonText"));
    vehicleModeButton = createTabButton(s("vehicleButtonText"));
    currentMode = AuthoringMode.LEVEL;
    currentLevelPaletteMode = LevelPaletteMode.TERRAIN;
    refreshCatalogPalettes();
  }

  private void subscribePalette(ComponentPaletteImpl palette) {
    palette.subscribe(canvas::setSelectedPlaceable);
    palette.subscribe(controller::setSelectedPlaceable);
  }

  private void wireEdgePan(ScrollPane scroll) {
    double threshold = d("edgePanThreshold");
    double speed = d("edgePanSpeed");
    double[] vel = {0, 0};

    javafx.event.EventHandler<MouseEvent> edgeDetect = e -> {
      double vw = scroll.getViewportBounds().getWidth();
      double vh = scroll.getViewportBounds().getHeight();
      vel[0] = edgeVelocity(e.getX(), vw, threshold, speed);
      vel[1] = edgeVelocity(e.getY(), vh, threshold, speed);
    };
    scroll.addEventFilter(MouseEvent.MOUSE_MOVED, edgeDetect);
    scroll.addEventFilter(MouseEvent.MOUSE_DRAGGED, edgeDetect);
    scroll.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
      vel[0] = 0;
      vel[1] = 0;
    });

    new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (vel[0] != 0) {
          scroll.setHvalue(clamp01(scroll.getHvalue() + vel[0]));
        }
        if (vel[1] != 0) {
          scroll.setVvalue(clamp01(scroll.getVvalue() + vel[1]));
        }
      }
    }.start();
  }

  private void wireZoomScroll(ScrollPane scroll) {
    scroll.addEventFilter(ScrollEvent.SCROLL, e -> {
      if (e.isControlDown()) {
        double step = d("zoomStep");
        double next = e.getDeltaY() > 0
            ? canvas.getZoom() * step
            : canvas.getZoom() / step;
        canvas.setZoom(next);
        e.consume();
      }
    });
  }

  private static double edgeVelocity(double pos, double size, double threshold, double speed) {
    if (pos < threshold && threshold > 0) {
      return -speed * (1 - pos / threshold);
    }
    if (pos > size - threshold && threshold > 0) {
      return speed * (1 - (size - pos) / threshold);
    }
    return 0;
  }

  private static double clamp01(double value) {
    return Math.max(0, Math.min(1, value));
  }

  private void fitCanvasToViewport() {
    Platform.runLater(() -> {
      if (canvasScrollPane == null) {
        return;
      }
      double viewportWidth = canvasScrollPane.getViewportBounds().getWidth();
      double canvasWidth = canvas.getVisualRoot().getBoundsInLocal().getWidth();
      if (viewportWidth > 0 && canvasWidth > 0) {
        canvas.setZoom(Math.min(1.0, viewportWidth / canvasWidth));
      }
    });
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
    settingsPaletteButton.setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.SETTINGS;
      refreshDisplayedMode();
    });
  }

  private HBox buildTopBar() {
    Button backButton = createSizedButton(s("backButtonText"), d("backButtonWidth"),
        d("backButtonHeight"));
    backButton.getStyleClass().add("authoring-back-button");
    backButton.setOnAction(event -> {
      if (!world.getObjects().isEmpty()) {
        boolean confirmed = alertService.showConfirmation(s("leaveWarningTitle"),
            s("leaveWarningMessage"));
        if (!confirmed) {
          return;
        }
      }
      onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY);
    });

    double actionWidth = d("topActionButtonWidth");
    double actionHeight = d("topActionButtonHeight");

    Button selectButton = createSizedButton(s("selectButtonText"), actionWidth, actionHeight);
    Button splineButton = createSizedButton(s("splineButtonText"), actionWidth, actionHeight);
    selectButton.getStyleClass().add("authoring-action-button");
    splineButton.getStyleClass().add("authoring-action-button");

    Button splineCommitButton = createSizedButton(
        s("splineCommitButtonText"), actionWidth * 0.7, actionHeight);
    splineCommitButton.getStyleClass().add("authoring-action-button");
    splineCommitButton.setOnAction(event -> {
      controller.getSplineDraft().commit();
      canvas.refreshGrid();
    });

    toolButtons = List.of(selectButton, splineButton);
    selectButton.getStyleClass().add("tool-active");

    selectButton.setOnAction(event -> {
      controller.setActiveTool(ToolType.SELECT);
      updateToolButtonStyles(selectButton);
    });
    splineButton.setOnAction(event -> {
      controller.setActiveTool(ToolType.SPLINE);
      updateToolButtonStyles(splineButton);
    });

    Button undoButton = createSizedButton(s("undoButtonText"), actionWidth, actionHeight);
    Button redoButton = createSizedButton(s("redoButtonText"), actionWidth, actionHeight);
    undoButton.getStyleClass().add("authoring-action-button");
    redoButton.getStyleClass().add("authoring-action-button");
    undoButton.setOnAction(event -> {
      controller.undo();
      terrainPropertiesPanel.hide();
    });
    redoButton.setOnAction(event -> {
      controller.redo();
      terrainPropertiesPanel.hide();
    });

    Button clearButton = createSizedButton(s("clearButtonText"), actionWidth, actionHeight);
    clearButton.getStyleClass().add("authoring-action-button");
    clearButton.setOnAction(event -> {
      if (!world.getObjects().isEmpty()) {
        boolean confirmed = alertService.showConfirmation(
            s("clearWarningTitle"), s("clearWarningMessage"));
        if (!confirmed) {
          return;
        }
      }
      resetForNewLevel();
    });

    double zoomButtonWidth = d("zoomButtonWidth");
    Button zoomInButton = createSizedButton(s("zoomInButtonText"), zoomButtonWidth, actionHeight);
    Button zoomOutButton =
        createSizedButton(s("zoomOutButtonText"), zoomButtonWidth, actionHeight);
    Button fitButton = createSizedButton(s("fitButtonText"), zoomButtonWidth * 1.5, actionHeight);
    zoomInButton.getStyleClass().add("authoring-action-button");
    zoomOutButton.getStyleClass().add("authoring-action-button");
    fitButton.getStyleClass().add("authoring-action-button");
    zoomInButton.setOnAction(event -> canvas.setZoom(canvas.getZoom() * d("zoomStep")));
    zoomOutButton.setOnAction(event -> canvas.setZoom(canvas.getZoom() / d("zoomStep")));
    fitButton.setOnAction(event -> fitCanvasToViewport());

    Button saveButton = createSizedButton(s("saveButtonText"), actionWidth, actionHeight);
    Button runButton = createSizedButton(s("runButtonText"), actionWidth, actionHeight);
    Button loadButton = createSizedButton(s("loadButtonText"), actionWidth, actionHeight);
    saveButton.getStyleClass().add("authoring-action-button");
    runButton.getStyleClass().add("authoring-action-button");
    loadButton.getStyleClass().add("authoring-action-button");
    saveButton.setOnAction(event -> handleSave(saveButton.getScene().getWindow()));
    loadButton.setOnAction(event -> handleLoad(loadButton.getScene().getWindow()));
    runButton.setOnAction(event -> handleRun());

    HBox toolButtonBox = new HBox(d("topBarSpacing"),
        selectButton, splineButton,
        splineCommitButton,
        zoomOutButton, zoomInButton, fitButton);
    toolButtonBox.setAlignment(Pos.CENTER_LEFT);

    HBox rightButtons = new HBox(
        d("topBarSpacing"), undoButton, redoButton, clearButton, saveButton, runButton, loadButton);
    rightButtons.setAlignment(Pos.CENTER_RIGHT);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(backButton, toolButtonBox, spacer, rightButtons);
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

  private HBox buildLevelPaletteTabs() {
    HBox tabs = new HBox(
        terrainPaletteButton,
        levelItemsPaletteButton,
        settingsPaletteButton);
    tabs.setAlignment(Pos.CENTER_LEFT);
    return tabs;
  }

  private void refreshCatalogPalettes() {
    if (terrainPalette == null || levelItemsPalette == null || vehiclePalette == null) {
      return;
    }
    if (gameConfig == null) {
      vehiclePalette.setAvailableComponentIds(List.of());
      terrainPalette.setItems(List.of());
      levelItemsPalette.setItems(List.of());
      return;
    }
    vehiclePalette.setAvailableComponentIds(gameConfig.components().stream()
        .map(component -> component.id())
        .toList());
    terrainPalette.setItems(gameConfig.terrainTypes().stream()
        .map(this::terrainEntry)
        .toList());
    levelItemsPalette.setItems(gameConfig.staticComponents().stream()
        .map(this::staticEntry)
        .toList());
  }

  private ComponentPaletteImpl.PaletteEntry terrainEntry(TerrainInfo info) {
    String color = info.visualColor() == null || info.visualColor().isBlank()
        ? DEFAULT_TERRAIN_COLOR : info.visualColor();
    String sprite = info.spriteUrl() == null ? "" : info.spriteUrl();
    return new ComponentPaletteImpl.PaletteEntry(info.id(), color, 1, 1, sprite, false,
        info.friction(), 0.2, false);
  }

  private ComponentPaletteImpl.PaletteEntry staticEntry(StaticComponentInfo info) {
    String sprite = info.spriteUrl() == null ? "" : info.spriteUrl();
    return new ComponentPaletteImpl.PaletteEntry(info.id(), DEFAULT_STATIC_COLOR, 1, 1, sprite,
        false, 0, 0, true);
  }

  private Label buildSelectedItemsLabel() {
    Label label = new Label();
    label.getStyleClass().add("authoring-selection-summary");
    label.setText(String.format(
        s("selectedItemsSummaryFormat"),
        s("selectedItemsNoneText")));
    return label;
  }

  private HBox buildPaletteModeRow() {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox row = new HBox(levelPaletteTabs, spacer, selectedItemsLabel);
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
    levelPaletteTabs.setVisible(isLevelMode);
    levelPaletteTabs.setManaged(isLevelMode);
    selectedItemsLabel.setVisible(!isLevelMode);
    selectedItemsLabel.setManaged(!isLevelMode);

    bottomContentHost.getChildren().clear();
    Node paletteNode;
    Pos alignment = Pos.TOP_LEFT;
    if (currentMode == AuthoringMode.VEHICLE) {
      paletteNode = vehiclePalette.getVisualRoot();
      alignment = Pos.CENTER;
    } else {
      paletteNode = switch (currentLevelPaletteMode) {
        case TERRAIN -> terrainPalette.getVisualRoot();
        case PIGS -> pigsPalette.getVisualRoot();
        case LEVEL_ITEMS -> levelItemsPalette.getVisualRoot();
        case SETTINGS -> levelAttributesPanel.getVisualRoot();
      };
    }
    StackPane.setAlignment(paletteNode, alignment);
    bottomContentHost.getChildren().add(paletteNode);
  }

  private void updateSelectedItemsSummary(String selectedItemsText) {
    selectedItemsLabel.setText(String.format(
        s("selectedItemsSummaryFormat"),
        selectedItemsText));
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

  private void handleRun() {
    try {
      controller.saveLevel(RUN_PREVIEW_PATH);
    } catch (LevelSaveException e) {
      alertService.showError(PROPS.getString("saveErrorTitle"), e.getMessage());
      return;
    }
    onRunLevel.accept(RUN_PREVIEW_PATH);
    onOutcome.accept(Outcome.OPEN_VEHICLE_BUILDER);
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

  private void updateToolButtonStyles(Button activeButton) {
    for (Button button : toolButtons) {
      button.getStyleClass().remove("tool-active");
    }
    activeButton.getStyleClass().add("tool-active");
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
