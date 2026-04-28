package oogasalad.view.authoring.internal;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.controller.authoring.internal.AiLevelGenerationCoordinator;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.LevelScanner;
import oogasalad.model.authoring.internal.GeneratedLevelModel;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Hub screen for the authoring environment. The left pane lists existing worlds with
 * "Edit Existing World" and "Add a New World" actions at the bottom; the right pane shows
 * the levels of the selected world (or a placeholder until one is picked) with an
 * "Add a New Level" action at the bottom.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 */
public class AuthoringEntryScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringEntry");
  private static final String LEVELS_DIR = "data/levels";

  private final Supplier<LevelScanner.ScanResult> scanResultSupplier;
  private LevelScanner.ScanResult scanResult;
  private final Consumer<String> onWorldSelected;
  private final Consumer<String> onLevelSelected;
  private final Consumer<String> onEditWorldSelected;
  private final Runnable onNewWorldSelected;

  private VBox worldsList;
  private VBox levelsList;
  private Label rightPlaceholder;
  private Button editExistingWorldButton;
  private String selectedWorldKey;
  private Button selectedWorldButton;

  /**
   * @param onOutcome       navigation callback fired by the hub's buttons
   * @param scanResult      filesystem scan listing worlds and their levels; populated into the left
   *                        pane on {@link #onEnter()}
   * @param onWorldSelected callback receiving the world key before opening the level editor
   * @param onLevelSelected callback receiving the absolute path of a level when the user picks one
   *                        from the right pane
   */
  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome,
      LevelScanner.ScanResult scanResult, Consumer<String> onWorldSelected,
      Consumer<String> onLevelSelected) {
    this(onOutcome, () -> scanResult, onWorldSelected, onLevelSelected);
  }

  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome,
      Supplier<LevelScanner.ScanResult> scanResultSupplier, Consumer<String> onWorldSelected,
      Consumer<String> onLevelSelected) {
    this(onOutcome, scanResultSupplier, onWorldSelected, onLevelSelected, worldKey -> {
    }, () -> {
    });
  }

  public AuthoringEntryScreenImpl(Consumer<Outcome> onOutcome,
      Supplier<LevelScanner.ScanResult> scanResultSupplier, Consumer<String> onWorldSelected,
      Consumer<String> onLevelSelected, Consumer<String> onEditWorldSelected,
      Runnable onNewWorldSelected) {
    super(onOutcome);
    this.scanResultSupplier = Objects.requireNonNull(scanResultSupplier);
    this.scanResult = this.scanResultSupplier.get();
    this.onWorldSelected = Objects.requireNonNull(onWorldSelected);
    this.onLevelSelected = Objects.requireNonNull(onLevelSelected);
    this.onEditWorldSelected = Objects.requireNonNull(onEditWorldSelected);
    this.onNewWorldSelected = Objects.requireNonNull(onNewWorldSelected);
  }

  @Override
  protected Parent createRoot() {
    BorderPane layout = buildRootPane();
    layout.setTop(buildTitle());
    layout.setCenter(buildSplitPane());
    layout.setBottom(wrapBottomLeft(buildBackButton()));
    return wrapInScroll(layout);
  }

  private ScrollPane wrapInScroll(BorderPane layout) {
    ScrollPane scroll = new ScrollPane(layout);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroll.getStyleClass().add("authoring-entry-scroll");
    return scroll;
  }

  @Override
  public void onEnter() {
    scanResult = scanResultSupplier.get();
    populateWorldButtons();
    showRightPlaceholder();
    updateEditWorldButtonState();
  }

  private BorderPane buildRootPane() {
    BorderPane pane = new BorderPane();
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-entry.css").toExternalForm());
    pane.getStyleClass().add("screen-root");
    pane.setPadding(new Insets(d("rootPadding")));
    return pane;
  }

  private Label buildTitle() {
    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));
    title.getStyleClass().add("authoring-entry-title");
    BorderPane.setAlignment(title, Pos.CENTER);
    BorderPane.setMargin(title, new Insets(0, 0, d("paneSpacing"), 0));
    return title;
  }

  private HBox buildSplitPane() {
    HBox split = new HBox(d("splitPaneSpacing"), buildLeftPane(), buildRightPane());
    split.setAlignment(Pos.TOP_CENTER);
    return split;
  }

  private VBox buildLeftPane() {
    Label header = headerLabel(s("worldsHeaderText"));
    worldsList = new VBox(d("paneSpacing"));
    worldsList.setAlignment(Pos.TOP_CENTER);

    editExistingWorldButton = actionButton(
        s("editExistingWorldButtonText"),
        event -> openSelectedWorldForEditing());
    editExistingWorldButton.setDisable(true);

    Button addNewWorldButton = actionButton(
        s("addNewWorldButtonText"),
        event -> openNewWorldForAuthoring());

    VBox bottomActions = new VBox(d("paneSpacing"), editExistingWorldButton, addNewWorldButton);
    bottomActions.setAlignment(Pos.TOP_CENTER);

    VBox left = new VBox(d("paneSpacing"), header, worldsList, spacer(), bottomActions);
    left.setAlignment(Pos.TOP_CENTER);
    left.setPadding(new Insets(d("buttonRegionPadding")));
    return left;
  }

  private VBox buildRightPane() {
    Label header = headerLabel(s("levelsHeaderText"));

    rightPlaceholder = new Label(s("rightPlaceholderText"));
    rightPlaceholder.setFont(Font.font(d("placeholderFontSize")));
    rightPlaceholder.getStyleClass().add("authoring-entry-placeholder");

    levelsList = new VBox(d("paneSpacing"));
    levelsList.setAlignment(Pos.TOP_CENTER);

    VBox right = new VBox(d("paneSpacing"), header, rightPlaceholder, levelsList);
    right.setAlignment(Pos.TOP_CENTER);
    right.setPadding(new Insets(d("buttonRegionPadding")));
    return right;
  }

  private void populateWorldButtons() {
    worldsList.getChildren().clear();
    Map<String, List<String>> worldsToLevels = scanResult.worldsToLevels();
    for (String worldKey : worldsToLevels.keySet()) {
      worldsList.getChildren().add(createWorldButton(worldKey));
    }
  }

  private Button createWorldButton(String worldKey) {
    String displayName = worldKey;
    String propKey = "world.button." + worldKey;
    if (PROPS.containsKey(propKey)) {
      displayName = PROPS.getString(propKey);
    }
    Button button = new Button(displayName);
    button.setPrefSize(d("worldButtonWidth"), d("worldButtonHeight"));
    button.getStyleClass().add("world-button");
    button.getStyleClass().add("world-" + worldKey);
    button.setOnAction(event -> selectWorld(button, worldKey));
    return button;
  }

  private void selectWorld(Button button, String worldKey) {
    if (selectedWorldButton != null) {
      selectedWorldButton.getStyleClass().remove("world-selected");
    }
    selectedWorldButton = button;
    selectedWorldKey = worldKey;
    selectedWorldButton.getStyleClass().add("world-selected");
    populateLevelsForSelectedWorld();
    updateEditWorldButtonState();
  }

  private void openSelectedWorldForEditing() {
    if (selectedWorldKey == null) {
      return;
    }
    onEditWorldSelected.accept(selectedWorldKey);
    onOutcome.accept(Outcome.OPEN_GAME_AUTHORING);
  }

  private void openNewWorldForAuthoring() {
    onNewWorldSelected.run();
    onOutcome.accept(Outcome.OPEN_GAME_AUTHORING);
  }

  private void populateLevelsForSelectedWorld() {
    rightPlaceholder.setVisible(false);
    rightPlaceholder.setManaged(false);
    levelsList.getChildren().clear();
    List<String> levels = scanResult.worldsToLevels().getOrDefault(selectedWorldKey, List.of());
    for (String levelKey : levels) {
      levelsList.getChildren().add(createLevelButton(levelKey));
    }
    levelsList.getChildren().add(buildLevelActions());
  }

  private Button createLevelButton(String levelKey) {
    Button button = new Button(levelKey);
    button.setPrefSize(d("levelButtonWidth"), d("levelButtonHeight"));
    button.getStyleClass().add("level-button");
    button.setOnAction(event -> openLevel(levelKey));
    return button;
  }

  private VBox buildLevelActions() {
    VBox actions = new VBox(d("paneSpacing"), createAddNewLevelButton(), createGenerateLevelButton());
    actions.setAlignment(Pos.TOP_CENTER);
    return actions;
  }

  private Button createAddNewLevelButton() {
    Button button = new Button(s("createLevelButtonText"));
    button.setPrefSize(d("levelButtonWidth"), d("levelButtonHeight"));
    button.getStyleClass().add("level-button");
    button.setOnAction(event -> {
      onWorldSelected.accept(selectedWorldKey);
      onOutcome.accept(Outcome.OPEN_AUTHORING);
    });
    return button;
  }

  private Button createGenerateLevelButton() {
    Button button = new Button(s("generateLevelButtonText"));
    button.setPrefSize(d("levelButtonWidth"), d("levelButtonHeight"));
    button.getStyleClass().add("level-button");
    button.setOnAction(event -> openAiLevelGeneration());
    return button;
  }

  private void openAiLevelGeneration() {
    AiLevelGenerationCoordinator coordinator = new AiLevelGenerationCoordinator(
        new GeneratedLevelModel(),
        result -> {
          onWorldSelected.accept(selectedWorldKey);
          onLevelSelected.accept(result.filePath());
          onOutcome.accept(Outcome.OPEN_AUTHORING);
        });
    coordinator.open(getRoot().getScene() == null ? null : getRoot().getScene().getWindow());
  }

  private void openLevel(String levelKey) {
    String path = Path.of(LEVELS_DIR, selectedWorldKey, levelKey + ".json").toString();
    onWorldSelected.accept(selectedWorldKey);
    onLevelSelected.accept(path);
    onOutcome.accept(Outcome.OPEN_AUTHORING);
  }

  private void showRightPlaceholder() {
    rightPlaceholder.setVisible(true);
    rightPlaceholder.setManaged(true);
    levelsList.getChildren().clear();
  }

  private void updateEditWorldButtonState() {
    editExistingWorldButton.setDisable(selectedWorldKey == null);
  }

  private Button buildBackButton() {
    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_MENU));
    return backButton;
  }

  private Button actionButton(String text, EventHandler<ActionEvent> onClick) {
    Button button = new Button(text);
    button.setFont(Font.font(d("actionFontSize")));
    button.setPrefSize(d("worldButtonWidth"), d("worldButtonHeight"));
    button.setWrapText(true);
    button.getStyleClass().add("authoring-entry-action");
    button.setOnAction(onClick);
    return button;
  }

  private Label headerLabel(String text) {
    Label header = new Label(text);
    header.setFont(Font.font(d("headerFontSize")));
    header.getStyleClass().add("authoring-entry-header");
    return header;
  }

  private Region spacer() {
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    return spacer;
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
