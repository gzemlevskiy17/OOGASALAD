package oogasalad.view.authoring.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.authoring.internal.VehicleComponentScrollPalette;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.constants.AuthoringScreenConstants;
import oogasalad.view.core.api.AppScreen;

/**
 * Layout shell for the authoring environment.
 */
public class AuthoringScreen implements AppScreen {

  private enum AuthoringMode {
    LEVEL,
    VEHICLE
  }

  private final Parent root;
  private final ScreenManager screenManager;
  private final ComponentPallete vehiclePalette;
  private final Pane levelPalettePlaceholder;
  private final StackPane bottomContentHost;
  private final Button levelModeButton;
  private final Button vehicleModeButton;

  private AuthoringMode currentMode;

  public AuthoringScreen(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.vehiclePalette = new VehicleComponentScrollPalette();
    this.levelPalettePlaceholder = new Pane();
    this.bottomContentHost = new StackPane();
    this.levelModeButton = createTabButton(AuthoringScreenConstants.LEVEL_BUTTON_TEXT);
    this.vehicleModeButton = createTabButton(AuthoringScreenConstants.VEHICLE_BUTTON_TEXT);
    this.currentMode = AuthoringMode.LEVEL;

    BorderPane layout = new BorderPane();
    layout.setPadding(new Insets(AuthoringScreenConstants.ROOT_PADDING));

    VBox topSections = new VBox(
        AuthoringScreenConstants.OUTER_SECTION_SPACING,
        buildTopBar(),
        buildScreenModeRow());

    VBox bottomSections = new VBox(
        AuthoringScreenConstants.OUTER_SECTION_SPACING,
        buildPaletteModeRow(),
        buildBottomRegion());

    layout.setTop(topSections);
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
        AuthoringScreenConstants.BACK_BUTTON_TEXT,
        AuthoringScreenConstants.BACK_BUTTON_WIDTH,
        AuthoringScreenConstants.BACK_BUTTON_HEIGHT);
    backButton.setOnAction(
        event -> this.screenManager.switchScreen(ScreenFactory.AUTHORING_ENTRY_SCREEN_KEY, null));

    Button saveButton = createSizedButton(
        AuthoringScreenConstants.SAVE_BUTTON_TEXT,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_WIDTH,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_HEIGHT);
    Button runButton = createSizedButton(
        AuthoringScreenConstants.RUN_BUTTON_TEXT,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_WIDTH,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_HEIGHT);
    Button loadButton = createSizedButton(
        AuthoringScreenConstants.LOAD_BUTTON_TEXT,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_WIDTH,
        AuthoringScreenConstants.TOP_ACTION_BUTTON_HEIGHT);

    HBox rightButtons = new HBox(
        AuthoringScreenConstants.TOP_BAR_SPACING, saveButton, runButton, loadButton);
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
        createTabButton(AuthoringScreenConstants.GENERAL_BUTTON_TEXT),
        createTabButton(AuthoringScreenConstants.PIGS_BUTTON_TEXT),
        createTabButton(AuthoringScreenConstants.LEVEL_ITEMS_BUTTON_TEXT),
        createTabButton(AuthoringScreenConstants.CUSTOM_BUTTON_TEXT));
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  private StackPane buildBottomRegion() {
    StackPane region = bottomContentHost;
    region.setPrefHeight(AuthoringScreenConstants.BOTTOM_REGION_HEIGHT);
    region.setMinHeight(AuthoringScreenConstants.BOTTOM_REGION_HEIGHT);
    region.setMinWidth(AuthoringScreenConstants.BOTTOM_REGION_MIN_WIDTH);
    return region;
  }

  private void refreshDisplayedMode() {
    bottomContentHost.getChildren().clear();
    if (currentMode == AuthoringMode.VEHICLE) {
      bottomContentHost.getChildren().add(vehiclePalette.getVisualRoot());
    } else {
      bottomContentHost.getChildren().add(levelPalettePlaceholder);
    }
  }

  private Button createTabButton(String text) {
    return createSizedButton(
        text,
        AuthoringScreenConstants.SMALL_TAB_BUTTON_WIDTH,
        AuthoringScreenConstants.SMALL_TAB_BUTTON_HEIGHT);
  }

  private Button createSizedButton(String text, double width, double height) {
    Button button = new Button(text);
    button.setPrefWidth(width);
    button.setPrefHeight(height);
    return button;
  }
}
