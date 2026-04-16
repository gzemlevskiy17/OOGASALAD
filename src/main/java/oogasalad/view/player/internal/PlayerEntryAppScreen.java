package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen on which the player chooses the world and the level they want to play
 * @author Iurii Beliaev
 */

public class PlayerEntryAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.PlayerEntryScreen");

  private final BiConsumer<String, String> onSelectionConfirmed;
  private Button selectedWorldButton;
  private Button selectedLevelButton;
  private Button startButton;
  private String selectedWorldKey;
  private String selectedLevelKey;

  public PlayerEntryAppScreen(Consumer<Outcome> onOutcome,
      BiConsumer<String, String> onSelectionConfirmed) {
    super(onOutcome);
    this.onSelectionConfirmed = onSelectionConfirmed;
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
      onSelectionConfirmed.accept(selectedWorldKey, selectedLevelKey);
      onOutcome.accept(Outcome.OPEN_VEHICLE_BUILDER);
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
    updateStartButtonState();
  }

  private void updateStartButtonState() {
    if (startButton != null) {
      startButton.setDisable(selectedWorldButton == null || selectedLevelButton == null);
    }
  }

  private HBox createLevelButtonRegion() {
    HBox levelBox = new HBox(d("level.region.spacing"));
    levelBox.setAlignment(Pos.CENTER);

    List<String> lockedLevels = getList("locked.levels");

    for (String level : getList("levels")) {
      Button btn = new Button(s("level.button." + level));
      btn.getStyleClass().add("level-button");

      if (lockedLevels.contains(level)) {
        btn.getStyleClass().add("locked-level");
        btn.setDisable(true);
      }

      if (!lockedLevels.contains(level)) {
        btn.setOnAction(e -> selectLevel(btn, level));
      }

      levelBox.getChildren().add(btn);
    }
    return levelBox;
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

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
