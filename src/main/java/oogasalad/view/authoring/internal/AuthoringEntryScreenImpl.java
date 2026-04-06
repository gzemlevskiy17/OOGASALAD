package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.constants.AuthoringEntryConstants;

/**
 * Entry screen for the authoring environment.
 */
public class AuthoringEntryScreenImpl implements AppScreen {

  private final Parent root;

  public AuthoringEntryScreenImpl(ScreenManager screenManager) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(AuthoringEntryConstants.ROOT_PADDING));

    Label title = new Label(AuthoringEntryConstants.TITLE_TEXT);
    title.setFont(Font.font(AuthoringEntryConstants.TITLE_FONT_SIZE));

    Button createNewLevelButton = new Button(AuthoringEntryConstants.CREATE_NEW_LEVEL_BUTTON_TEXT);
    configureActionButton(createNewLevelButton);
    createNewLevelButton.setOnAction(
        event -> screenManager.switchScreen(ScreenFactory.AUTHORING_SCREEN_KEY, null));

    Button loadExistingLevelButton = new Button(
        AuthoringEntryConstants.LOAD_EXISTING_LEVEL_BUTTON_TEXT);
    configureActionButton(loadExistingLevelButton);

    HBox buttonRegion = new HBox(AuthoringEntryConstants.BUTTON_REGION_SPACING,
        createNewLevelButton, loadExistingLevelButton);
    buttonRegion.setAlignment(Pos.CENTER);
    buttonRegion.setPadding(new Insets(AuthoringEntryConstants.BUTTON_REGION_PADDING));

    VBox centerContent = new VBox(AuthoringEntryConstants.TITLE_SECTION_SPACING, title, buttonRegion);
    centerContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(centerContent);

    Button backButton = new Button(AuthoringEntryConstants.BACK_BUTTON_TEXT);
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));

    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(AuthoringEntryConstants.ACTION_FONT_SIZE));
    button.setPrefSize(AuthoringEntryConstants.ACTION_BUTTON_WIDTH,
        AuthoringEntryConstants.ACTION_BUTTON_HEIGHT);
    button.setWrapText(true);
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
