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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
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
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.core.api.AppScreen;

/**
 * Layout shell for the authoring environment.
 */
public class AuthoringScreenImpl implements AppScreen {

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

  private final Parent root;
  private final AuthoringControllerImpl controller;
  private final Consumer<Outcome> onOutcome;
  private final StackPane canvasHost;
  private final ComponentPallete vehiclePalette;
  private final ComponentPaletteImpl terrainPalette;
  private final ComponentPaletteImpl pigsPalette;
  private final ComponentPaletteImpl levelItemsPalette;
  private final ComponentPaletteImpl customPalette;
  private final AuthoringAlertService alertService;
  private final FileChooserService fileChooserService;
  private final HBox paletteModeRow;
  private final StackPane bottomContentHost;
  private final Button levelModeButton;
  private final Button vehicleModeButton;
  private final Button terrainPaletteButton;
  private final Button pigsPaletteButton;
  private final Button levelItemsPaletteButton;
  private final Button customPaletteButton;
  private final Button settingsPaletteButton;
  private final LevelAttributesPanel levelAttributesPanel;
  private final LevelAttributes levelAttributes;
  private final TerrainPropertiesPanel terrainPropertiesPanel;
  private final World world;
  private final AuthoringCanvasImpl canvas;
  private ScrollPane canvasScrollPane;

  private List<Button> toolButtons;
  private AuthoringMode currentMode;
  private LevelPaletteMode currentLevelPaletteMode;

  public AuthoringScreenImpl(Consumer<Outcome> onOutcome) {
    this.onOutcome = onOutcome;
    this.world = new WorldImpl();
    this.levelAttributes = new LevelAttributes();
    this.levelAttributesPanel = new LevelAttributesPanel(levelAttributes);
    this.terrainPropertiesPanel = new TerrainPropertiesPanel();
    this.canvas = buildCanvas();
    this.alertService = new AuthoringAlertService();
    this.fileChooserService = new FileChooserService();
    this.controller = buildController();
    this.canvasHost = buildCanvasHost();
    this.vehiclePalette = new VehicleComponentScrollPalette();
    this.terrainPalette = new ComponentPaletteImpl();
    this.pigsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.PigsPalette");
    this.levelItemsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.LevelItemsPalette");
    this.customPalette = new ComponentPaletteImpl("oogasalad.view.authoring.CustomPalette");
    wirePaletteSubscriptions();
    this.terrainPaletteButton = createTabButton(s("terrainButtonText"));
    this.pigsPaletteButton = createTabButton(s("pigsButtonText"));
    this.levelItemsPaletteButton = createTabButton(s("levelItemsButtonText"));
    this.customPaletteButton = createTabButton(s("customButtonText"));
    this.settingsPaletteButton = createTabButton(s("settingsButtonText"));
    this.paletteModeRow = buildPaletteModeRow();
    this.bottomContentHost = new StackPane();
    this.levelModeButton = createTabButton(s("levelButtonText"));
    this.vehicleModeButton = createTabButton(s("vehicleButtonText"));
    this.currentMode = AuthoringMode.LEVEL;
    this.currentLevelPaletteMode = LevelPaletteMode.TERRAIN;
    this.root = buildLayout();
    wireButtonHandlers();
    refreshDisplayedMode();

    controller.setToolChangeListener(type -> {
      if (type == ToolType.DRAW) {
        updateToolButtonStyles(toolButtons.get(0));
      }
    });
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    resetForNewLevel();
    canvas.setZoom(1.0);
  }

  public void resetForNewLevel() {
    controller.clearLevel();
    terrainPropertiesPanel.hide();
  }

  public void loadLevelFromPath(String path) {
    try {
      controller.loadLevel(path);
    } catch (LevelLoadException e) {
      alertService.showError(PROPS.getString("loadErrorTitle"), e.getMessage());
    }
  }

  private AuthoringCanvasImpl buildCanvas() {
    AuthoringCanvasImpl c = new AuthoringCanvasImpl(world);
    c.setOnObjectSelected(terrainPropertiesPanel::show);
    return c;
  }

  private AuthoringControllerImpl buildController() {
    // JSON is the handoff between the authoring editor (grid-cell coordinates)
    // and the gameplay runtime (world-pixel coordinates). Passing cellSize to
    // both saver and loader centralizes that conversion in the data layer, so
    // authored levels land on the ground the vehicle actually spawns on
    // instead of appearing as a tiny glyph in the top-left corner.
    double cellSize = canvas.getCellSize();
    AuthoringControllerImpl c = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(cellSize), new LevelSaverImpl(cellSize),
        levelAttributes, new ToolSelectorImpl(), canvas);
    c.setPlacementErrorHandler(
        message -> alertService.showWarning(s("placementWarningTitle"), message));
    c.initializeCanvasHandlers();
    return c;
  }

  private StackPane buildCanvasHost() {
    canvasScrollPane = new ScrollPane(canvas.getVisualRoot());
    canvasScrollPane.setFitToWidth(false);
    canvasScrollPane.setFitToHeight(false);
    canvasScrollPane.setPannable(false);
    canvasScrollPane.getStyleClass().add("authoring-canvas-scroll");
    wireEdgePan(canvasScrollPane);
    wireZoomScroll(canvasScrollPane);
    return new StackPane(canvasScrollPane);
  }

  private void wireEdgePan(ScrollPane scroll) {
    double threshold = d("edgePanThreshold");
    double speed = d("edgePanSpeed");
    double[] vel = {0, 0};

    // MOUSE_MOVED fires when hovering; MOUSE_DRAGGED fires while drawing (button held).
    // Both must be tracked so edge pan works during active block placement.
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

  private static double clamp01(double v) {
    return Math.max(0, Math.min(1, v));
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

  private void wirePaletteSubscriptions() {
    terrainPalette.subscribe(canvas::setSelectedPlaceable);
    terrainPalette.subscribe(controller::setSelectedPlaceable);
    pigsPalette.subscribe(canvas::setSelectedPlaceable);
    pigsPalette.subscribe(controller::setSelectedPlaceable);
    levelItemsPalette.subscribe(canvas::setSelectedPlaceable);
    levelItemsPalette.subscribe(controller::setSelectedPlaceable);
    customPalette.subscribe(canvas::setSelectedPlaceable);
    customPalette.subscribe(controller::setSelectedPlaceable);
  }

  // ---------------------------------------------------------------------------
  // Layout builders
  // ---------------------------------------------------------------------------

  private BorderPane buildLayout() {
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
    return layout;
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

    Button drawButton = createSizedButton(s("drawButtonText"), actionWidth, actionHeight);
    Button eraseButton = createSizedButton(s("eraseButtonText"), actionWidth, actionHeight);
    Button paintButton = createSizedButton(s("paintButtonText"), actionWidth, actionHeight);
    Button splineButton = createSizedButton(s("splineButtonText"), actionWidth, actionHeight);
    drawButton.getStyleClass().add("authoring-action-button");
    eraseButton.getStyleClass().add("authoring-action-button");
    paintButton.getStyleClass().add("authoring-action-button");
    splineButton.getStyleClass().add("authoring-action-button");

    // Resolution spinner only influences future tessellations; shown inline with the tools and
    // kept visible regardless of active tool so the designer can tune the value in advance.
    Spinner<Integer> splineResolutionSpinner = new Spinner<>();
    splineResolutionSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(
            1, 64, controller.getSplineDraft().getResolution(), 1));
    splineResolutionSpinner.setEditable(true);
    splineResolutionSpinner.setPrefWidth(72);
    splineResolutionSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        controller.getSplineDraft().setResolution(newVal);
      }
    });
    Label splineResolutionLabel = new Label(s("splineResolutionLabel"));
    Button splineCommitButton = createSizedButton(
        s("splineCommitButtonText"), actionWidth * 0.7, actionHeight);
    splineCommitButton.getStyleClass().add("authoring-action-button");
    splineCommitButton.setOnAction(event -> {
      controller.getSplineDraft().commit();
      canvas.refreshGrid();
    });

    toolButtons = List.of(drawButton, eraseButton, paintButton, splineButton);
    drawButton.getStyleClass().add("tool-active");

    drawButton.setOnAction(event -> {
      controller.setActiveTool(ToolType.DRAW);
      updateToolButtonStyles(drawButton);
    });
    eraseButton.setOnAction(event -> {
      controller.setActiveTool(ToolType.ERASE);
      updateToolButtonStyles(eraseButton);
    });
    paintButton.setOnAction(event -> {
      controller.setActiveTool(ToolType.PAINT);
      updateToolButtonStyles(paintButton);
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
    Button zoomOutButton = createSizedButton(s("zoomOutButtonText"), zoomButtonWidth, actionHeight);
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

    HBox toolButtonBox = new HBox(d("topBarSpacing"),
        drawButton, eraseButton, paintButton, splineButton,
        splineResolutionLabel, splineResolutionSpinner, splineCommitButton,
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
    double bottomHeight = d("bottomRegionHeight");
    bottomContentHost.setPrefHeight(bottomHeight);
    bottomContentHost.setMinHeight(bottomHeight);
    bottomContentHost.setMinWidth(d("bottomRegionMinWidth"));
    bottomContentHost.setAlignment(Pos.TOP_LEFT);
    bottomContentHost.getStyleClass().add("authoring-bottom-region");
    return bottomContentHost;
  }

  // ---------------------------------------------------------------------------
  // Button handlers
  // ---------------------------------------------------------------------------

  private void wireButtonHandlers() {
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

  // ---------------------------------------------------------------------------
  // File operations
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // Button factory helpers
  // ---------------------------------------------------------------------------

  private Button createTabButton(String text) {
    Button button = createSizedButton(text, d("smallTabButtonWidth"), d("smallTabButtonHeight"));
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
    for (Button btn : toolButtons) {
      btn.getStyleClass().remove("tool-active");
    }
    activeButton.getStyleClass().add("tool-active");
  }

  // ---------------------------------------------------------------------------
  // Property helpers
  // ---------------------------------------------------------------------------

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
