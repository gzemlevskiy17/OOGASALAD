package oogasalad.view.player.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.LevelId;
import oogasalad.data.api.LevelLockedException;
import oogasalad.data.internal.LevelScanner;
import oogasalad.view.core.api.AbstractAppScreen;

/**
 * Screen on which the player chooses the world and the level they want to play
 * @author Iurii Beliaev
 */

public class PlayerEntryAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.PlayerEntryScreen");

  /** Sentinel world key for the "Others" bucket — files at the root of {@code data/levels/}. */
  public static final String OTHERS_WORLD_KEY = "others";

  private final BiConsumer<String, String> onSelectionConfirmed;
  private final Supplier<LevelScanner.ScanResult> levelCatalogSupplier;
  private Map<String, List<String>> displayedLevelsByWorld;
  // The list of JSON filenames (with .json extension) reported by LevelScanner's
  // ScanResult.others(). Each becomes one button under the "Others" world; basename-without-.json
  // is used for both the button label and the levelKey passed to onSelectionConfirmed.
  private List<String> otherLevelFiles;

  // Wrapper HBox that gets cleared and re-populated when the world changes. Assigned inside
  // createLevelButtonRegion (called via the super constructor's createRoot, which runs BEFORE
  // subclass field initializers, so a field initializer here would still be null at use time).
  private HBox worldBox;
  private HBox levelButtonsContainer;
  private Button selectedWorldButton;
  private Button selectedLevelButton;
  private Button startButton;
  private String selectedWorldKey;
  private String selectedLevelKey;
  // Captured while populating world buttons; used by onEnter to auto-select the first world.
  private Button firstWorldButton;
  private String firstWorldKey;
  public PlayerEntryAppScreen(Consumer<Outcome> onOutcome,
      BiConsumer<String, String> onSelectionConfirmed,
      Map<String, List<String>> displayedLevelsByWorld,
      List<String> otherLevelFiles) {
    this(onOutcome, onSelectionConfirmed,
        () -> new LevelScanner.ScanResult(displayedLevelsByWorld, otherLevelFiles));
  }

  public PlayerEntryAppScreen(Consumer<Outcome> onOutcome,
      BiConsumer<String, String> onSelectionConfirmed,
      Supplier<LevelScanner.ScanResult> levelCatalogSupplier) {
    super(onOutcome);
    this.onSelectionConfirmed = onSelectionConfirmed;
    this.levelCatalogSupplier = levelCatalogSupplier;
    this.displayedLevelsByWorld = Map.of();
    this.otherLevelFiles = List.of();
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(
        getClass().getResource("/oogasalad/view/player/player-entry.css").toExternalForm()
    );
    root.getStyleClass().add("screen-root");

    VBox mainContent = new VBox(d("main.content.spacing"));
    mainContent.setAlignment(Pos.CENTER);

    mainContent.getChildren().addAll(
        createLevelChoiceContentRegion(),
        createStartBadPiggiesContentRegion()
    );

    root.setCenter(mainContent);
    root.setBottom(createBackButtonRegion());

    return root;
  }

  /**
   * Lifecycle hook from {@link oogasalad.view.core.api.AppScreen}, called by the screen
   * manager every time this screen becomes active. World buttons are populated here because
   * {@code AbstractAppScreen}'s constructor calls {@code createRoot()} before this constructor
   * assigns the scanned level catalog.
   */
  @Override
  public void onEnter() {
    refreshLevelCatalog();
    populateWorldButtons();
    // Auto-select the first world only when nothing is currently selected — first entry, or
    // a state where the previous selection got cleared. Re-entries that already have a
    // selection (user came back from gameplay) are left alone.
    if (selectedWorldButton == null && firstWorldButton != null) {
      selectWorld(firstWorldButton, firstWorldKey);
    }
  }

  private void refreshLevelCatalog() {
    LevelScanner.ScanResult scanResult = levelCatalogSupplier.get();
    displayedLevelsByWorld = copyLevelMap(scanResult.worldsToLevels());
    otherLevelFiles = List.copyOf(scanResult.others());
    selectedWorldButton = null;
    selectedWorldKey = null;
    selectedLevelButton = null;
    selectedLevelKey = null;
    if (levelButtonsContainer != null) {
      levelButtonsContainer.getChildren().clear();
    }
    updateStartButtonState();
  }

  private VBox createLevelChoiceContentRegion() {
    VBox container = new VBox(d("level.choice.spacing"));
    container.getStyleClass().add("selection-region");
    container.setAlignment(Pos.TOP_CENTER);

    container.getChildren().addAll(
        createTitleRegion(),
        createWorldButtonRegion(),
        createLevelButtonRegion(),
        createCommentRegion()
    );
    return container;
  }

  private VBox createStartBadPiggiesContentRegion() {
    startButton = new Button(s("start.button.text"));
    startButton.getStyleClass().add("start-button");
    startButton.setDisable(true);

    startButton.setOnAction(event -> {
      try {
        onSelectionConfirmed.accept(selectedWorldKey, selectedLevelKey);
        onOutcome.accept(Outcome.OPEN_VEHICLE_BUILDER);
      } catch (LevelLockedException e) {
        showLockedAlert(e.attempted(), e.furthestReached());
      }
    });

    VBox container = new VBox(startButton);
    container.getStyleClass().add("selection-region");
    container.setAlignment(Pos.CENTER);
    return container;
  }

  private HBox createWorldButtonRegion() {
    worldBox = new HBox(d("world.region.spacing"));
    worldBox.setAlignment(Pos.CENTER);
    return worldBox;
  }

  private void populateWorldButtons() {
    if (worldBox == null) {
      return;
    }
    worldBox.getChildren().clear();
    firstWorldButton = null;
    firstWorldKey = null;
    for (String world : displayedLevelsByWorld.keySet()) {
      addWorldButton(world, worldLabel(world));
    }
    if (!otherLevelFiles.isEmpty()) {
      addWorldButton(OTHERS_WORLD_KEY, worldLabel(OTHERS_WORLD_KEY));
    }
  }

  private void addWorldButton(String worldKey, String label) {
    Button btn = new Button(label);
    btn.getStyleClass().add("world-button");
    btn.getStyleClass().add("world-" + worldKey);
    btn.setOnAction(e -> selectWorld(btn, worldKey));
    worldBox.getChildren().add(btn);
    if (firstWorldButton == null) {
      firstWorldButton = btn;
      firstWorldKey = worldKey;
    }
  }

  private void selectWorld(Button btn, String worldKey) {
    if (selectedWorldButton != null) {
      selectedWorldButton.getStyleClass().remove("world-selected");
    }
    selectedWorldButton = btn;
    selectedWorldKey = worldKey;
    selectedWorldButton.getStyleClass().add("world-selected");
    refreshLevelButtons(worldKey);
    updateStartButtonState();
  }

  private void updateStartButtonState() {
    if (startButton != null) {
      startButton.setDisable(selectedWorldButton == null || selectedLevelButton == null);
    }
  }

  private HBox createLevelButtonRegion() {
    levelButtonsContainer = new HBox(d("level.region.spacing"));
    levelButtonsContainer.setAlignment(Pos.CENTER);
    // Empty until a world is picked — selectWorld populates it via refreshLevelButtons.
    return levelButtonsContainer;
  }

  /**
   * Rebuilds the level button region for the chosen world. Always clears any prior selection
   * (the previous buttons are gone from the tree). For scanned worlds, renders the levels found
   * on disk. For the "Others" world, renders a horizontal scroll pane of one button per file in
   * {@link #otherLevelFiles} — basename text, no progression coupling.
   */
  private void refreshLevelButtons(String worldKey) {
    selectedLevelButton = null;
    selectedLevelKey = null;
    levelButtonsContainer.getChildren().clear();
    if (OTHERS_WORLD_KEY.equals(worldKey)) {
      populateOthersLevels();
    } else {
      populateStandardLevels(worldKey);
    }
  }

  private void populateStandardLevels(String worldKey) {
    List<String> available = displayedLevelsByWorld.getOrDefault(worldKey, List.of());
    for (String level : available) {
      Button btn = new Button(levelLabel(level));
      btn.getStyleClass().add("level-button");
      btn.setOnAction(e -> selectLevel(btn, level));
      levelButtonsContainer.getChildren().add(btn);
    }
  }

  private void populateOthersLevels() {
    HBox row = new HBox(d("level.region.spacing"));
    row.setAlignment(Pos.CENTER);
    row.setPadding(new javafx.geometry.Insets(0, d("level.region.padding"), 0,
        d("level.region.padding")));
    for (String filename : otherLevelFiles) {
      String basename = stripJsonExtension(filename);
      Button btn = new Button(basename);
      btn.getStyleClass().add("level-button");
      btn.setOnAction(e -> selectLevel(btn, basename));
      row.getChildren().add(btn);
    }
    ScrollPane scroll = new ScrollPane(row);
    scroll.getStyleClass().add("others-scroll");
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setFitToHeight(true);
    scroll.setPannable(true);
    scroll.setMaxWidth(d("level.region.others.width"));
    // Make the ScrollPane visually transparent so the row of buttons doesn't sit on a grey
    // panel — the rest of the level region has no background of its own.
    scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    levelButtonsContainer.getChildren().add(scroll);
  }

  private static String stripJsonExtension(String filename) {
    return filename.endsWith(".json")
        ? filename.substring(0, filename.length() - ".json".length())
        : filename;
  }

  private void selectLevel(Button btn, String levelKey) {
    if (selectedLevelButton != null) {
      selectedLevelButton.getStyleClass().remove("level-selected");
    }
    selectedLevelButton = btn;
    selectedLevelKey = levelKey;
    selectedLevelButton.getStyleClass().add("level-selected");
    updateStartButtonState();
  }

  private Label createTitleRegion() {
    Label title = new Label(s("title.text"));
    title.getStyleClass().add("header-title");
    return title;
  }

  private Text createCommentRegion() {
    Text comment = new Text(s("comment.text"));
    comment.getStyleClass().add("comment-text");
    return comment;
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_MENU));

    StackPane container = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("nav-region");
    return container;
  }

  /**
   * Pops an INFORMATION alert when the user tries to play a locked level. Mirrors the minimal
   * {@code AuthoringAlertService} pattern so we don't introduce a separate helper class for
   * just one popup.
   */
  private void showLockedAlert(LevelId attempted, LevelId furthestReached) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle("Level Locked");
    alert.setHeaderText("Level " + attempted.worldKey() + "/" + attempted.levelKey()
        + " is locked");
    alert.setContentText(furthestReached == null
        ? "You need to complete level 1 first."
        : "You've reached " + furthestReached.worldKey() + "/" + furthestReached.levelKey()
            + ". Complete the next level to continue.");
    alert.showAndWait();
  }

  private static Map<String, List<String>> copyLevelMap(Map<String, List<String>> levelsByWorld) {
    Map<String, List<String>> copy = new LinkedHashMap<>();
    levelsByWorld.forEach((world, levels) -> copy.put(world, List.copyOf(levels)));
    return Collections.unmodifiableMap(copy);
  }

  private String worldLabel(String worldKey) {
    String key = "world.button." + worldKey;
    return PROPS.containsKey(key) ? PROPS.getString(key) : worldKey;
  }

  private String levelLabel(String levelKey) {
    String key = "level.button." + levelKey;
    return PROPS.containsKey(key) ? PROPS.getString(key) : levelKey;
  }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
