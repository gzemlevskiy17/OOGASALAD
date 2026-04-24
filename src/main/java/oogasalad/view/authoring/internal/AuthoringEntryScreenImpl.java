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
import oogasalad.view.core.api.AppScreen;

/**
 * Entry screen for the authoring environment.
 */
public class AuthoringEntryScreenImpl implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringEntry");

  private final Parent root;

  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome) {
    BorderPane layout = buildRootPane();
    layout.setCenter(buildCenterContent(onOutcome));
    layout.setBottom(wrapBottomLeft(buildBackButton(onOutcome)));
    this.root = layout;
  }

  private BorderPane buildRootPane() {
    BorderPane pane = new BorderPane();
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-entry.css").toExternalForm());
    pane.getStyleClass().add("screen-root");
    pane.setPadding(new Insets(d("rootPadding")));
    return pane;
  }

  private VBox buildCenterContent(Consumer<Outcome> onOutcome) {
    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));
    title.getStyleClass().add("authoring-entry-title");

    VBox centerContent = new VBox(d("titleSectionSpacing"), title, buildButtonRow(onOutcome));
    centerContent.setAlignment(Pos.TOP_CENTER);
    centerContent.getStyleClass().add("authoring-entry-center");
    return centerContent;
  }

  private HBox buildButtonRow(Consumer<Outcome> onOutcome) {
    Button createNewLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createNewLevelButton);
    createNewLevelButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_AUTHORING));

    Button loadExistingLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadExistingLevelButton);

    HBox buttonRow = new HBox(d("buttonRegionSpacing"), createNewLevelButton,
        loadExistingLevelButton);
    buttonRow.setAlignment(Pos.CENTER);
    buttonRow.setPadding(new Insets(d("buttonRegionPadding")));
    buttonRow.getStyleClass().add("authoring-entry-button-row");
    return buttonRow;
  }

  private Button buildBackButton(Consumer<Outcome> onOutcome) {
    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_MENU));
    return backButton;
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
