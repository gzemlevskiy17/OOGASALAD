package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
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
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Entry screen for the authoring environment. Lets the user start a new game catalog or open
 * an existing one before they can author levels.
 */
public class AuthoringEntryScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringEntry");

  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  @Override
  protected Parent createRoot() {
    BorderPane layout = buildRootPane();
    layout.setCenter(buildCenterContent());
    layout.setBottom(wrapBottomLeft(buildBackButton()));
    return layout;
  }

  private BorderPane buildRootPane() {
    BorderPane pane = new BorderPane();
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-entry.css").toExternalForm());
    pane.getStyleClass().add("screen-root");
    pane.setPadding(new Insets(d("rootPadding")));
    return pane;
  }

  private VBox buildCenterContent() {
    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));
    title.getStyleClass().add("authoring-entry-title");

    VBox centerContent = new VBox(d("titleSectionSpacing"), title, buildButtonRow());
    centerContent.setAlignment(Pos.TOP_CENTER);
    centerContent.getStyleClass().add("authoring-entry-center");
    return centerContent;
  }

  private HBox buildButtonRow() {
    Button createNewGameButton = new Button(s("createNewGameButtonText"));
    configureActionButton(createNewGameButton);
    createNewGameButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_GAME_AUTHORING));

    Button openExistingGameButton = new Button(s("openExistingGameButtonText"));
    configureActionButton(openExistingGameButton);
    openExistingGameButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_GAME_AUTHORING));

    HBox buttonRow = new HBox(
        d("buttonRegionSpacing"),
        createNewGameButton,
        openExistingGameButton);
    buttonRow.setAlignment(Pos.CENTER);
    buttonRow.setPadding(new Insets(d("buttonRegionPadding")));
    buttonRow.getStyleClass().add("authoring-entry-button-row");
    return buttonRow;
  }

  private Button buildBackButton() {
    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_MENU));
    return backButton;
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(d("actionFontSize")));
    button.setPrefSize(d("actionButtonWidth"), d("actionButtonHeight"));
    button.setWrapText(true);
    button.getStyleClass().add("authoring-entry-action");
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("authoring-entry-nav");
    return container;
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
