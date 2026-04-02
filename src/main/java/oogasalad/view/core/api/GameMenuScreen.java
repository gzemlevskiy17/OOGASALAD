package oogasalad.view.core.api;

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

/**
 * Basic start menu screen for the application.
 */
public class GameMenuScreen implements AppScreen {

  private static final double SCENE_WIDTH = 1000;
  private static final double SCENE_HEIGHT = 700;
  private static final double TITLE_FONT_SIZE = 42;
  private static final double BUTTON_SPACING = 18;
  private static final double BUTTON_WIDTH = 180;

  private final Scene scene;

  public GameMenuScreen() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(24));

    Button settingsButton = new Button("Settings");
    root.setTop(wrapTopLeft(settingsButton));

    Label title = new Label("Good Piggies");
    title.setFont(Font.font(TITLE_FONT_SIZE));

    Button playButton = new Button("Play");
    Button createButton = new Button("Create");
    playButton.setPrefWidth(BUTTON_WIDTH);
    createButton.setPrefWidth(BUTTON_WIDTH);

    VBox centerContent = new VBox(BUTTON_SPACING, title, playButton, createButton);
    centerContent.setAlignment(Pos.CENTER);
    root.setCenter(centerContent);

    Button loginButton = new Button("Login");
    root.setBottom(wrapBottomLeft(loginButton));

    scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
  }

  @Override
  public Scene getScene() {
    return scene;
  }

  private HBox wrapTopLeft(Button button) {
    HBox container = new HBox(button);
    container.setAlignment(Pos.TOP_LEFT);
    return container;
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
