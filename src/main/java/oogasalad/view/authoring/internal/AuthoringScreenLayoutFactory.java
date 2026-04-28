package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Builds the authoring screen shell and wires top-level UI controls to supplied callbacks.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
final class AuthoringScreenLayoutFactory {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private final Node canvasHost;
  private final Node terrainPropertiesRoot;
  private final Runnable backHandler;
  private final Runnable commitSplineHandler;
  private final Runnable undoHandler;
  private final Runnable redoHandler;
  private final Runnable clearHandler;
  private final Runnable zoomInHandler;
  private final Runnable zoomOutHandler;
  private final Runnable fitCanvasHandler;
  private final Consumer<Window> saveHandler;
  private final Runnable runHandler;
  private final Consumer<Window> loadHandler;

  AuthoringScreenLayoutFactory(Node canvasHost, Node terrainPropertiesRoot, Runnable backHandler,
      Runnable commitSplineHandler, Runnable undoHandler, Runnable redoHandler,
      Runnable clearHandler, Runnable zoomInHandler, Runnable zoomOutHandler,
      Runnable fitCanvasHandler, Consumer<Window> saveHandler, Runnable runHandler,
      Consumer<Window> loadHandler) {
    this.canvasHost = canvasHost;
    this.terrainPropertiesRoot = terrainPropertiesRoot;
    this.backHandler = backHandler;
    this.commitSplineHandler = commitSplineHandler;
    this.undoHandler = undoHandler;
    this.redoHandler = redoHandler;
    this.clearHandler = clearHandler;
    this.zoomInHandler = zoomInHandler;
    this.zoomOutHandler = zoomOutHandler;
    this.fitCanvasHandler = fitCanvasHandler;
    this.saveHandler = saveHandler;
    this.runHandler = runHandler;
    this.loadHandler = loadHandler;
  }

  Parts build() {
    Button levelModeButton = createTabButton(s("levelButtonText"));
    Button vehicleModeButton = createTabButton(s("vehicleButtonText"));
    Button terrainPaletteButton = createTabButton(s("terrainButtonText"));
    Button pigsPaletteButton = createTabButton(s("pigsButtonText"));
    Button levelItemsPaletteButton = createTabButton(s("levelItemsButtonText"));
    Button settingsPaletteButton = createTabButton(s("settingsButtonText"));
    Label selectedItemsLabel = buildSelectedItemsLabel();
    HBox levelPaletteTabs = buildLevelPaletteTabs(
        terrainPaletteButton, levelItemsPaletteButton, settingsPaletteButton);
    StackPane bottomContentHost = buildBottomRegion();

    TopBarParts topBarParts = buildTopBar();
    VBox topSections = new VBox(d("outerSectionSpacing"), topBarParts.topBar(),
        buildScreenModeRow(levelModeButton, vehicleModeButton));
    topSections.getStyleClass().add("authoring-top-sections");

    HBox paletteModeRow = buildPaletteModeRow(levelPaletteTabs, selectedItemsLabel);
    VBox bottomSections = new VBox(d("outerSectionSpacing"), paletteModeRow, bottomContentHost);
    bottomSections.getStyleClass().add("authoring-bottom-sections");

    BorderPane layout = buildRootPane();
    layout.setTop(topSections);
    layout.setCenter(canvasHost);
    layout.setBottom(bottomSections);
    layout.setRight(terrainPropertiesRoot);

    return new Parts(layout, topBarParts.selectButton(), topBarParts.splineButton(),
        levelModeButton, vehicleModeButton, terrainPaletteButton, pigsPaletteButton,
        levelItemsPaletteButton, settingsPaletteButton, levelPaletteTabs, selectedItemsLabel,
        bottomContentHost);
  }

  private BorderPane buildRootPane() {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-screen.css").toExternalForm());
    layout.getStyleClass().add("screen-root");
    layout.setPadding(new Insets(d("rootPadding")));
    return layout;
  }

  private TopBarParts buildTopBar() {
    Button backButton = createSizedButton(s("backButtonText"), d("backButtonWidth"),
        d("backButtonHeight"));
    backButton.getStyleClass().add("authoring-back-button");
    backButton.setOnAction(event -> backHandler.run());

    double actionWidth = d("topActionButtonWidth");
    double actionHeight = d("topActionButtonHeight");

    Button selectButton = createActionButton(s("selectButtonText"), actionWidth, actionHeight);
    Button splineButton = createActionButton(s("splineButtonText"), actionWidth, actionHeight);
    Button splineCommitButton = createActionButton(
        s("splineCommitButtonText"), actionWidth * 0.7, actionHeight);
    splineCommitButton.setOnAction(event -> commitSplineHandler.run());

    Button undoButton = createActionButton(s("undoButtonText"), actionWidth, actionHeight);
    Button redoButton = createActionButton(s("redoButtonText"), actionWidth, actionHeight);
    undoButton.setOnAction(event -> undoHandler.run());
    redoButton.setOnAction(event -> redoHandler.run());

    Button clearButton = createActionButton(s("clearButtonText"), actionWidth, actionHeight);
    clearButton.setOnAction(event -> clearHandler.run());

    double zoomButtonWidth = d("zoomButtonWidth");
    Button zoomInButton = createActionButton(s("zoomInButtonText"), zoomButtonWidth, actionHeight);
    Button zoomOutButton = createActionButton(s("zoomOutButtonText"), zoomButtonWidth,
        actionHeight);
    Button fitButton = createActionButton(s("fitButtonText"), zoomButtonWidth * 1.5,
        actionHeight);
    zoomInButton.setOnAction(event -> zoomInHandler.run());
    zoomOutButton.setOnAction(event -> zoomOutHandler.run());
    fitButton.setOnAction(event -> fitCanvasHandler.run());

    Button saveButton = createActionButton(s("saveButtonText"), actionWidth, actionHeight);
    Button runButton = createActionButton(s("runButtonText"), actionWidth, actionHeight);
    Button loadButton = createActionButton(s("loadButtonText"), actionWidth, actionHeight);
    saveButton.setOnAction(event -> saveHandler.accept(saveButton.getScene().getWindow()));
    runButton.setOnAction(event -> runHandler.run());
    loadButton.setOnAction(event -> loadHandler.accept(loadButton.getScene().getWindow()));

    HBox toolButtonBox = new HBox(d("topBarSpacing"),
        selectButton, splineButton, splineCommitButton, zoomOutButton, zoomInButton, fitButton);
    toolButtonBox.setAlignment(Pos.CENTER_LEFT);

    HBox rightButtons = new HBox(
        d("topBarSpacing"), undoButton, redoButton, clearButton, saveButton, runButton, loadButton);
    rightButtons.setAlignment(Pos.CENTER_RIGHT);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(backButton, toolButtonBox, spacer, rightButtons);
    topBar.setAlignment(Pos.CENTER_LEFT);
    topBar.getStyleClass().add("authoring-top-bar");
    return new TopBarParts(topBar, selectButton, splineButton);
  }

  private HBox buildScreenModeRow(Button levelModeButton, Button vehicleModeButton) {
    HBox row = new HBox(levelModeButton, vehicleModeButton);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("authoring-screen-mode-row");
    return row;
  }

  private HBox buildLevelPaletteTabs(Button terrainPaletteButton, Button levelItemsPaletteButton,
      Button settingsPaletteButton) {
    HBox tabs = new HBox(terrainPaletteButton, levelItemsPaletteButton, settingsPaletteButton);
    tabs.setAlignment(Pos.CENTER_LEFT);
    return tabs;
  }

  private Label buildSelectedItemsLabel() {
    Label label = new Label();
    label.getStyleClass().add("authoring-selection-summary");
    label.setText(String.format(
        s("selectedItemsSummaryFormat"),
        s("selectedItemsNoneText")));
    return label;
  }

  private HBox buildPaletteModeRow(HBox levelPaletteTabs, Label selectedItemsLabel) {
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox row = new HBox(levelPaletteTabs, spacer, selectedItemsLabel);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("authoring-palette-mode-row");
    return row;
  }

  private StackPane buildBottomRegion() {
    StackPane region = new StackPane();
    double bottomHeight = d("bottomRegionHeight");
    region.setPrefHeight(bottomHeight);
    region.setMinHeight(bottomHeight);
    region.setMinWidth(d("bottomRegionMinWidth"));
    region.setAlignment(Pos.TOP_LEFT);
    region.getStyleClass().add("authoring-bottom-region");
    return region;
  }

  private Button createActionButton(String text, double width, double height) {
    Button button = createSizedButton(text, width, height);
    button.getStyleClass().add("authoring-action-button");
    return button;
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

  record Parts(Parent root, Button selectButton, Button splineButton,
               Button levelModeButton, Button vehicleModeButton, Button terrainPaletteButton,
               Button pigsPaletteButton, Button levelItemsPaletteButton,
               Button settingsPaletteButton, HBox levelPaletteTabs, Label selectedItemsLabel,
               StackPane bottomContentHost) {
  }

  private record TopBarParts(HBox topBar, Button selectButton, Button splineButton) {
  }
}
