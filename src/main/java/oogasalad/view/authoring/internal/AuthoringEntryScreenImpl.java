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
 * Entry screen for the authoring environment.
 *
 * @author Emilio Garcia
 */
public class AuthoringEntryScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringEntry");
  private static final ThreadLocal<Consumer<String>> PENDING_LOAD_HANDLER = new ThreadLocal<>();
  private static final ThreadLocal<FileChooserService> PENDING_FILE_CHOOSER = new ThreadLocal<>();

  private final Consumer<String> loadLevelHandler;
  private final FileChooserService fileChooserService;

  /**
   * Creates the authoring entry screen with no-op load behavior.
   *
   * @param onOutcome callback used to request screen navigation
   * @author Emilio Garcia
   */
  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome) {
    this(onOutcome, path -> {
    });
  }

  /**
   * Creates the authoring entry screen with a callback that will be invoked when the user selects
   * an existing level file to load.
   *
   * @param onOutcome callback used to request screen navigation
   * @param loadLevelHandler callback that receives the selected file path before opening authoring
   * @author Emilio Garcia
   */
  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler) {
    super(prepare(onOutcome, loadLevelHandler));
    this.loadLevelHandler = consumeLoadHandler();
    this.fileChooserService = consumeFileChooserService();
  }

  private static Consumer<Outcome> prepare(Consumer<Outcome> onOutcome,
      Consumer<String> loadLevelHandler) {
    PENDING_LOAD_HANDLER.set(loadLevelHandler);
    PENDING_FILE_CHOOSER.set(new FileChooserService());
    return onOutcome;
  }

  private static Consumer<String> consumeLoadHandler() {
    Consumer<String> handler = PENDING_LOAD_HANDLER.get();
    PENDING_LOAD_HANDLER.remove();
    return handler == null ? path -> {
    } : handler;
  }

  private static FileChooserService consumeFileChooserService() {
    FileChooserService chooserService = PENDING_FILE_CHOOSER.get();
    PENDING_FILE_CHOOSER.remove();
    return chooserService == null ? new FileChooserService() : chooserService;
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
    Button createNewLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createNewLevelButton);
    createNewLevelButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_AUTHORING));

    Button loadExistingLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadExistingLevelButton);
    loadExistingLevelButton.setOnAction(event -> fileChooserService
        .chooseLoadFile(loadExistingLevelButton.getScene().getWindow())
        .ifPresent(file -> {
          loadLevelHandler.accept(file.getAbsolutePath());
          onOutcome.accept(Outcome.OPEN_AUTHORING);
        }));

    HBox buttonRow = new HBox(
        d("buttonRegionSpacing"),
        createNewLevelButton,
        loadExistingLevelButton);
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
