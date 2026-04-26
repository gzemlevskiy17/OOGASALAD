package oogasalad.view.authoring.internal;

import java.util.Objects;
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
 * Skeleton screen for editing a game catalog (game.json). The catalog editor itself is not
 * implemented yet; this screen currently routes the user to the level-authoring screen for
 * creating or loading levels of the current game.
 *
 * @author Paulina Vvedenskaya
 */
public class GameAuthoringScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.GameAuthoring");

  private final Consumer<String> loadLevelHandler;
  private final FileChooserService fileChooserService;

  /**
   * @param onOutcome        navigation callback fired by the screen's buttons
   * @param loadLevelHandler invoked with the absolute path of a level file when the user picks one
   *                         from the file chooser
   */
  public GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler) {
    super(onOutcome);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.fileChooserService = new FileChooserService();
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

    Label placeholder = new Label(s("catalogPlaceholderText"));
    placeholder.setFont(Font.font(d("placeholderFontSize")));
    placeholder.getStyleClass().add("authoring-entry-placeholder");

    VBox centerContent = new VBox(
        d("titleSectionSpacing"), title, placeholder, buildButtonRow());
    centerContent.setAlignment(Pos.TOP_CENTER);
    centerContent.getStyleClass().add("authoring-entry-center");
    return centerContent;
  }

  private HBox buildButtonRow() {
    Button createNewLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createNewLevelButton);
    createNewLevelButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_AUTHORING));

    Button loadExistingLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadExistingLevelButton);
    loadExistingLevelButton.setOnAction(event -> handleLoadLevel(loadExistingLevelButton));

    HBox buttonRow = new HBox(
        d("buttonRegionSpacing"),
        createNewLevelButton,
        loadExistingLevelButton);
    buttonRow.setAlignment(Pos.CENTER);
    buttonRow.setPadding(new Insets(d("buttonRegionPadding")));
    buttonRow.getStyleClass().add("authoring-entry-button-row");
    return buttonRow;
  }

  private void handleLoadLevel(Button source) {
    fileChooserService
        .chooseLoadFile(source.getScene().getWindow())
        .ifPresent(file -> {
          loadLevelHandler.accept(file.getAbsolutePath());
          onOutcome.accept(Outcome.OPEN_AUTHORING);
        });
  }

  private Button buildBackButton() {
    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY));
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
