package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
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

/**
 * Entry screen for the authoring environment.
 */
public class AuthoringEntryScreenImpl implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringEntry");

  private final Parent root;

  public AuthoringEntryScreenImpl(ScreenManager screenManager) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(d("rootPadding")));

    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));

    Button createNewLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createNewLevelButton);
    createNewLevelButton.setOnAction(
        event -> screenManager.switchScreen(ScreenFactory.AUTHORING_SCREEN_KEY, null));

    Button loadExistingLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadExistingLevelButton);

    HBox buttonRegion = new HBox(
        d("buttonRegionSpacing"),
        createNewLevelButton, loadExistingLevelButton);
    buttonRegion.setAlignment(Pos.CENTER);
    buttonRegion.setPadding(new Insets(d("buttonRegionPadding")));

    VBox centerContent = new VBox(
        d("titleSectionSpacing"), title, buttonRegion);
    centerContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(centerContent);

    Button backButton = new Button(s("backButtonText"));
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));

    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(d("actionFontSize")));
    button.setPrefSize(
        d("actionButtonWidth"),
        d("actionButtonHeight"));
    button.setWrapText(true);
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
