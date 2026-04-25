package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import oogasalad.data.internal.LevelProgression;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen on which the player chooses the world and the level they want to play
 * @author Iurii Beliaev
 */

public class PlayerEntryAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.PlayerEntryScreen");

  /** Sentinel world key for the "Others" bucket — files at the root of {@code data/levels/}. */
  public static final String OTHERS_WORLD_KEY = "others";

  private final BiConsumer<String, String> onSelectionConfirmed;
  private final LevelProgression progression;
  // The list of root-level JSON filenames (with .json extension) reported by LevelScanner's
  // ScanResult.others(). Each becomes one button under the "Others" world; basename-without-.json
  // is used for both the button label and the levelKey passed to onSelectionConfirmed.
  private final List<String> otherLevelFiles;

  // Wrapper HBox that gets cleared and re-populated when the world changes. Assigned inside
  // createLevelButtonRegion (called via the super constructor's createRoot, which runs BEFORE
  // subclass field initializers, so a field initializer here would still be null at use time).
  private HBox levelButtonsContainer;
  private Button selectedWorldButton;
  private Button selectedLevelButton;
  private Button startButton;
  private String selectedWorldKey;
  private String selectedLevelKey;
  // Captured during createWorldButtonRegion; used to auto-select the first world after createRoot
  // finishes so the user sees a populated level region instead of a blank one.
  private Button firstWorldButton;
  private String firstWorldKey;

  public PlayerEntryAppScreen(Consumer<Outcome> onOutcome,
      BiConsumer<String, String> onSelectionConfirmed,
      LevelProgression progression,
      List<String> otherLevelFiles) {
    super(onOutcome);
    this.onSelectionConfirmed = onSelectionConfirmed;
    this.progression = progression;
    this.otherLevelFiles = List.copyOf(otherLevelFiles);
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

    // Auto-select the first world so the level region isn't blank on entry. Equivalent to a
    // synthetic click on the first world button — selectWorld handles styling, level rebuild,
    // and start-button state. Done here (after every region exists) rather than inside
    // createWorldButtonRegion, where levelButtonsContainer hadn't been created yet.
    if (firstWorldButton != null) {
      selectWorld(firstWorldButton, firstWorldKey);
    }

    return root;
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
    HBox worldBox = new HBox(d("world.region.spacing"));
    worldBox.setAlignment(Pos.CENTER);

    for (String world : getList("worlds")) {
      Button btn = new Button(s("world.button." + world));
      btn.getStyleClass().add("world-button");
      btn.getStyleClass().add("world-" + world);
      worldBox.getChildren().add(btn);

      btn.setOnAction(e -> selectWorld(btn, world));
      // Remember the first world button so createRoot can auto-select it once everything is
      // built — gives the user a populated level region on entry instead of a blank space.
      if (firstWorldButton == null) {
        firstWorldButton = btn;
        firstWorldKey = world;
      }
    }

    // "Others" world button only appears when there's something to show. Hidden when the
    // others/ folder is empty or missing.
    if (!otherLevelFiles.isEmpty()) {
      Button othersBtn = new Button(s("world.button.others"));
      othersBtn.getStyleClass().add("world-button");
      othersBtn.getStyleClass().add("world-" + OTHERS_WORLD_KEY);
      othersBtn.setOnAction(e -> selectWorld(othersBtn, OTHERS_WORLD_KEY));
      worldBox.getChildren().add(othersBtn);
    }

    return worldBox;
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
   * (the previous buttons are gone from the tree). For canonical worlds, renders the labelled
   * buttons from properties with disable state driven by {@code progression.levelsIn}. For the
   * "Others" world, renders a horizontal scroll pane of one button per file in
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
    List<String> available = progression.levelsIn(worldKey);
    for (String level : getList("levels")) {
      Button btn = new Button(s("level.button." + level));
      btn.getStyleClass().add("level-button");
      boolean enabled = available.contains(level);
      btn.setDisable(!enabled);
      if (!enabled) {
        btn.getStyleClass().add("locked-level");
      }
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

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
