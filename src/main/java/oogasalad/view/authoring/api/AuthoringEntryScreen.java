package oogasalad.view.authoring.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.view.core.api.AppScreen;

/**
 * Entry screen for the authoring environment.
 */
public class AuthoringEntryScreen implements AppScreen {

  private static final double SCENE_WIDTH = 1000;
  private static final double SCENE_HEIGHT = 700;
  private static final double ROOT_PADDING = 24;
  private static final double TITLE_FONT_SIZE = 36;
  private static final double ACTION_FONT_SIZE = 22;
  private static final double TITLE_SECTION_SPACING = 28;
  private static final double BUTTON_REGION_SPACING = 24;
  private static final double BUTTON_REGION_PADDING = 32;
  private static final double ACTION_BUTTON_WIDTH = 300;
  private static final double ACTION_BUTTON_HEIGHT = 180;

  private final Scene scene;

  public AuthoringEntryScreen() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(ROOT_PADDING));

    Label title = new Label("Build Your Piggies");
    title.setFont(Font.font(TITLE_FONT_SIZE));

    Button createNewLevelButton = new Button("Create a New Level");
    configureActionButton(createNewLevelButton);

    Button loadExistingLevelButton = new Button("Load and Edit on Existing Level");
    configureActionButton(loadExistingLevelButton);

    HBox buttonRegion = new HBox(BUTTON_REGION_SPACING, createNewLevelButton, loadExistingLevelButton);
    buttonRegion.setAlignment(Pos.CENTER);
    buttonRegion.setPadding(new Insets(BUTTON_REGION_PADDING));

    VBox centerContent = new VBox(TITLE_SECTION_SPACING, title, buttonRegion);
    centerContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(centerContent);

    Button backButton = new Button("Back");
    root.setBottom(wrapBottomLeft(backButton));

    scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
  }

  @Override
  public Scene getScene() {
    return scene;
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(ACTION_FONT_SIZE));
    button.setPrefSize(ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
    button.setWrapText(true);
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
