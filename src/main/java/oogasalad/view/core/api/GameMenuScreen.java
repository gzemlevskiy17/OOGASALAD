package oogasalad.view.core.api;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;

/**
 * Basic start menu screen for the application.
 */
public class GameMenuScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.core.GameMenu");

  private final Parent root;

  public GameMenuScreen(ScreenManager screenManager) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(Double.parseDouble(PROPS.getString("rootPadding"))));

    Button settingsButton = new Button(PROPS.getString("settingsButtonText"));
    Button helpButton = new Button(PROPS.getString("helpButtonText"));
    Button profileButton = new Button(PROPS.getString("profileButtonText"));
    root.setTop(wrapTopLeft(settingsButton, helpButton, profileButton));

    Label title = new Label(PROPS.getString("menuTitle"));
    title.setFont(Font.font(Double.parseDouble(PROPS.getString("titleFontSize"))));

    double buttonWidth = Double.parseDouble(PROPS.getString("buttonWidth"));

    Button playButton = new Button(PROPS.getString("playButtonText"));
    playButton.setPrefWidth(buttonWidth);
    playButton.setOnAction(event ->
        screenManager.switchScreen(ScreenFactory.PLAYER_ENTRY_APP_SCREEN_KEY, null));

    Button createButton = new Button(PROPS.getString("createButtonText"));
    createButton.setPrefWidth(buttonWidth);
    createButton.setOnAction(event ->
        screenManager.switchScreen(ScreenFactory.AUTHORING_ENTRY_SCREEN_KEY, null));

    VBox centerContent = new VBox(
        Double.parseDouble(PROPS.getString("buttonSpacing")), title, playButton, createButton);
    centerContent.setAlignment(Pos.CENTER);
    root.setCenter(centerContent);

    Button loginButton = new Button(PROPS.getString("loginButtonText"));
    root.setBottom(wrapBottomLeft(loginButton));

    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private VBox wrapTopLeft(Button... buttons) {
    VBox container = new VBox(
        Double.parseDouble(PROPS.getString("cornerButtonSpacing")), buttons);
    container.setAlignment(Pos.TOP_LEFT);
    return container;
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
