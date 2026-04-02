package oogasalad.view.core.api;

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
import oogasalad.view.constants.GameMenuConstants;

/**
 * Basic start menu screen for the application.
 */
public class GameMenuScreen implements AppScreen {

  private final Parent root;

  public GameMenuScreen(ScreenManager screenManager) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(GameMenuConstants.ROOT_PADDING));

    Button settingsButton = new Button(GameMenuConstants.SETTINGS_BUTTON_TEXT);
    Button helpButton = new Button(GameMenuConstants.HELP_BUTTON_TEXT);
    Button profileButton = new Button(GameMenuConstants.PROFILE_BUTTON_TEXT);
    root.setTop(wrapTopLeft(settingsButton, helpButton, profileButton));

    Label title = new Label(GameMenuConstants.MENU_TITLE);
    title.setFont(Font.font(GameMenuConstants.TITLE_FONT_SIZE));

    Button playButton = new Button(GameMenuConstants.PLAY_BUTTON_TEXT);
    Button createButton = new Button(GameMenuConstants.CREATE_BUTTON_TEXT);
    playButton.setPrefWidth(GameMenuConstants.BUTTON_WIDTH);
    createButton.setPrefWidth(GameMenuConstants.BUTTON_WIDTH);
    createButton.setOnAction(event ->
        screenManager.switchScreen(ScreenFactory.AUTHORING_ENTRY_SCREEN_KEY, null));

    VBox centerContent = new VBox(GameMenuConstants.BUTTON_SPACING, title, playButton, createButton);
    centerContent.setAlignment(Pos.CENTER);
    root.setCenter(centerContent);

    Button loginButton = new Button(GameMenuConstants.LOGIN_BUTTON_TEXT);
    root.setBottom(wrapBottomLeft(loginButton));

    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private VBox wrapTopLeft(Button... buttons) {
    VBox container = new VBox(GameMenuConstants.CORNER_BUTTON_SPACING, buttons);
    container.setAlignment(Pos.TOP_LEFT);
    return container;
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
