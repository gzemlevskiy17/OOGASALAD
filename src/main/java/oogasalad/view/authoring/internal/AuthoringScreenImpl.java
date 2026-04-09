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
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.controller.authoring.internal.ToolSelectorImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelSaverImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
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
    CUSTOM
  }

  private final Parent root;
  private final Consumer<Outcome> onOutcome;
  private final StackPane canvasHost;
  private final ComponentPallete vehiclePalette;
  private final ComponentPaletteImpl terrainPalette;
  private final ComponentPaletteImpl pigsPalette;
  private final ComponentPaletteImpl levelItemsPalette;
  private final ComponentPaletteImpl customPalette;
  private final AuthoringAlertService alertService;
  private final HBox paletteModeRow;
  private final StackPane bottomContentHost;
  private final Button levelModeButton;
  private final Button vehicleModeButton;
  private final Button terrainPaletteButton;
  private final Button pigsPaletteButton;
  private final Button levelItemsPaletteButton;
  private final Button customPaletteButton;

  private AuthoringMode currentMode;
  private LevelPaletteMode currentLevelPaletteMode;

  public AuthoringScreenImpl(Consumer<Outcome> onOutcome) {
    this.onOutcome = onOutcome;
    // TODO: replace with existing or shared World instance
    World world = new WorldImpl();
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl(world);
    AuthoringControllerImpl controller = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(), new LevelSaverImpl(),
        null, new ToolSelectorImpl(), canvas);
    this.alertService = new AuthoringAlertService();
    controller.setPlacementErrorHandler(
        message -> alertService.showWarning(s("placementWarningTitle"), message));
    controller.initializeCanvasHandlers();
    ScrollPane canvasScrollPane = new ScrollPane(canvas.getVisualRoot());
    canvasScrollPane.setFitToWidth(false);
    canvasScrollPane.setFitToHeight(false);
    canvasScrollPane.setPannable(true);
    canvasScrollPane.getStyleClass().add("authoring-canvas-scroll");
    this.canvasHost = new StackPane(canvasScrollPane);
    this.vehiclePalette = new VehicleComponentScrollPalette();
    this.terrainPalette = new ComponentPaletteImpl();
    this.pigsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.PigsPalette");
    this.levelItemsPalette = new ComponentPaletteImpl("oogasalad.view.authoring.LevelItemsPalette");
    this.customPalette = new ComponentPaletteImpl("oogasalad.view.authoring.CustomPalette");
    terrainPalette.subscribe(canvas::setSelectedPlaceable);
    terrainPalette.subscribe(controller::setSelectedPlaceable);
    pigsPalette.subscribe(canvas::setSelectedPlaceable);
    pigsPalette.subscribe(controller::setSelectedPlaceable);
    levelItemsPalette.subscribe(canvas::setSelectedPlaceable);
    levelItemsPalette.subscribe(controller::setSelectedPlaceable);
    customPalette.subscribe(canvas::setSelectedPlaceable);
    customPalette.subscribe(controller::setSelectedPlaceable);
    this.terrainPaletteButton = createTabButton(s("terrainButtonText"));
    this.pigsPaletteButton = createTabButton(s("pigsButtonText"));
    this.levelItemsPaletteButton = createTabButton(s("levelItemsButtonText"));
    this.customPaletteButton = createTabButton(s("customButtonText"));
    this.paletteModeRow = buildPaletteModeRow();
    this.bottomContentHost = new StackPane();
    this.levelModeButton = createTabButton(s("levelButtonText"));
    this.vehicleModeButton = createTabButton(s("vehicleButtonText"));
    this.currentMode = AuthoringMode.LEVEL;
    this.currentLevelPaletteMode = LevelPaletteMode.TERRAIN;

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

    this.root = layout;

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

    refreshDisplayedMode();
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private HBox buildTopBar() {
    Button backButton = createSizedButton(
        s("backButtonText"),
        d("backButtonWidth"),
        d("backButtonHeight"));
    backButton.getStyleClass().add("authoring-back-button");
    backButton.setOnAction(
        event -> onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY));

    double actionWidth = d("topActionButtonWidth");
    double actionHeight = d("topActionButtonHeight");
    Button saveButton = createSizedButton(s("saveButtonText"), actionWidth, actionHeight);
    Button runButton = createSizedButton(s("runButtonText"), actionWidth, actionHeight);
    Button loadButton = createSizedButton(s("loadButtonText"), actionWidth, actionHeight);
    saveButton.getStyleClass().add("authoring-action-button");
    runButton.getStyleClass().add("authoring-action-button");
    loadButton.getStyleClass().add("authoring-action-button");

    HBox rightButtons = new HBox(
        d("topBarSpacing"), saveButton, runButton, loadButton);
    rightButtons.setAlignment(Pos.CENTER_RIGHT);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(backButton, spacer, rightButtons);
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
        customPaletteButton);
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
      };
    }
    StackPane.setAlignment(paletteNode, Pos.TOP_LEFT);
    bottomContentHost.getChildren().add(paletteNode);
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
