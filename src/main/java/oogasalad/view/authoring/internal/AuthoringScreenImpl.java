package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.controller.authoring.internal.ToolSelectorImpl;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
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

  private final Parent root;
  private final ScreenManager screenManager;
  private final StackPane canvasHost;
  private final ComponentPallete vehiclePalette;
  private final ComponentPaletteImpl terrainPalette;
  private final HBox paletteModeRow;
  private final StackPane bottomContentHost;
  private final Button levelModeButton;
  private final Button vehicleModeButton;

  private AuthoringMode currentMode;

  public AuthoringScreenImpl(ScreenManager screenManager) {
    this.screenManager = screenManager;
    // TODO: replace with existing or shared World instance
    World world = new WorldImpl();
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl(world);
    AuthoringControllerImpl controller = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(), new LevelSaverImpl(),
        null, new ToolSelectorImpl(), canvas);
    controller.initializeCanvasHandlers();
    this.canvasHost = new StackPane(canvas.getVisualRoot());
    this.vehiclePalette = new VehicleComponentScrollPalette();
    this.terrainPalette = new ComponentPaletteImpl();
    terrainPalette.subscribe(canvas::setSelectedPlaceable);
    terrainPalette.subscribe(controller::setSelectedPlaceable);
    this.paletteModeRow = buildPaletteModeRow();
    this.bottomContentHost = new StackPane();
    this.levelModeButton = createTabButton(PROPS.getString("levelButtonText"));
    this.vehicleModeButton = createTabButton(PROPS.getString("vehicleButtonText"));
    this.currentMode = AuthoringMode.LEVEL;

    BorderPane layout = new BorderPane();
    layout.setPadding(new Insets(Double.parseDouble(PROPS.getString("rootPadding"))));

    VBox topSections = new VBox(
        Double.parseDouble(PROPS.getString("outerSectionSpacing")),
        buildTopBar(),
        buildScreenModeRow());

    VBox bottomSections = new VBox(
        Double.parseDouble(PROPS.getString("outerSectionSpacing")),
        paletteModeRow,
        buildBottomRegion());

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

    refreshDisplayedMode();
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private HBox buildTopBar() {
    Button backButton = createSizedButton(
        PROPS.getString("backButtonText"),
        Double.parseDouble(PROPS.getString("backButtonWidth")),
        Double.parseDouble(PROPS.getString("backButtonHeight")));
    backButton.setOnAction(
        event -> this.screenManager.switchScreen(ScreenFactory.AUTHORING_ENTRY_SCREEN_KEY, null));

    double actionWidth = Double.parseDouble(PROPS.getString("topActionButtonWidth"));
    double actionHeight = Double.parseDouble(PROPS.getString("topActionButtonHeight"));
    Button saveButton = createSizedButton(PROPS.getString("saveButtonText"), actionWidth, actionHeight);
    Button runButton = createSizedButton(PROPS.getString("runButtonText"), actionWidth, actionHeight);
    Button loadButton = createSizedButton(PROPS.getString("loadButtonText"), actionWidth, actionHeight);

    HBox rightButtons = new HBox(
        Double.parseDouble(PROPS.getString("topBarSpacing")), saveButton, runButton, loadButton);
    rightButtons.setAlignment(Pos.CENTER_RIGHT);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox topBar = new HBox(backButton, spacer, rightButtons);
    topBar.setAlignment(Pos.CENTER_LEFT);
    return topBar;
  }

  private HBox buildScreenModeRow() {
    HBox row = new HBox(levelModeButton, vehicleModeButton);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private HBox buildPaletteModeRow() {
    HBox row = new HBox(
        createTabButton(PROPS.getString("generalButtonText")),
        createTabButton(PROPS.getString("pigsButtonText")),
        createTabButton(PROPS.getString("levelItemsButtonText")),
        createTabButton(PROPS.getString("customButtonText")));
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private StackPane buildBottomRegion() {
    StackPane region = bottomContentHost;
    double bottomHeight = Double.parseDouble(PROPS.getString("bottomRegionHeight"));
    region.setPrefHeight(bottomHeight);
    region.setMinHeight(bottomHeight);
    region.setMinWidth(Double.parseDouble(PROPS.getString("bottomRegionMinWidth")));
    return region;
  }

  private void refreshDisplayedMode() {
    boolean isLevelMode = currentMode == AuthoringMode.LEVEL;
    paletteModeRow.setVisible(isLevelMode);
    paletteModeRow.setManaged(isLevelMode);

    bottomContentHost.getChildren().clear();
    if (currentMode == AuthoringMode.VEHICLE) {
      bottomContentHost.getChildren().add(vehiclePalette.getVisualRoot());
    } else {
      bottomContentHost.getChildren().add(terrainPalette.getVisualRoot());
    }
  }

  private Button createTabButton(String text) {
    return createSizedButton(
        text,
        Double.parseDouble(PROPS.getString("smallTabButtonWidth")),
        Double.parseDouble(PROPS.getString("smallTabButtonHeight")));
  }

  private Button createSizedButton(String text, double width, double height) {
    Button button = new Button(text);
    button.setPrefWidth(width);
    button.setPrefHeight(height);
    return button;
  }
}
