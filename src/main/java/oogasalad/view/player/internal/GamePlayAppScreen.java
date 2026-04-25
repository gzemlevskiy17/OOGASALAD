package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;
import oogasalad.view.player.api.GameplayView;

/**
 * Screen for the main window of the gameplay
 * @author Iurii Beliaev
 */

public class GamePlayAppScreen extends AbstractAppScreen implements GameplayView {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");

  private Pane gameLayer;
  private HBox bottomControls;
  private GameHudDisplay hud;
  private Runnable onPause = () -> { };
  private Runnable onResume = () -> { };
  private Runnable onReset = () -> { };
  private java.util.function.Consumer<String> onToolActivated = id -> {
  };
  private java.util.function.Consumer<String> onToolDeactivated = id -> {
  };

  public GamePlayAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  public Pane getGameLayer() {
    return gameLayer;
  }

  /**
   * Pushes the latest per-tick stats to the in-game HUD. Intended to be wired to
   * {@code GameplaySession.setOnStatsTick}.
   *
   * @param elapsedSeconds seconds since session start (>= 0)
   * @param progressPercent progress toward the goal in [0.0, 1.0]
   */
  public void updateHud(double elapsedSeconds, double progressPercent) {
    if (hud != null) {
      hud.update(elapsedSeconds, progressPercent);
    }
  }

  public void setOnPause(Runnable onPause) {
    if (onPause == null) {
      this.onPause = () -> { };
    } else {
      this.onPause = onPause;
    }
  }

  public void setOnResume(Runnable onResume) {
    if (onResume == null) {
      this.onResume = () -> { };
    } else {
      this.onResume = onResume;
    }
  }

  public void setOnReset(Runnable onReset) {
    if (onReset == null) {
      this.onReset = () -> { };
    } else {
      this.onReset = onReset;
    }
  }

  public void setOnToolActivated(java.util.function.Consumer<String> onToolActivated) {
    this.onToolActivated = onToolActivated != null ? onToolActivated : id -> {
    };
  }

  public void setOnToolDeactivated(java.util.function.Consumer<String> onToolDeactivated) {
    this.onToolDeactivated = onToolDeactivated != null ? onToolDeactivated : id -> {
    };
  }

  @Override
  protected StackPane createRoot() {
    StackPane root = new StackPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/gameplay.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    gameLayer = createGameAreaRegion();
    gameLayer.prefWidthProperty().bind(root.widthProperty());
    gameLayer.prefHeightProperty().bind(root.heightProperty());

    HBox topControls = createTopControlsRegion();
    bottomControls = createBottomControlsRegion(null);
    hud = new GameHudDisplay();

    Node backButton = createBackButtonRegion();

    StackPane.setAlignment(topControls, Pos.TOP_LEFT);
    StackPane.setAlignment(hud, Pos.TOP_CENTER);
    StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
    StackPane.setAlignment(bottomControls, Pos.BOTTOM_CENTER);

    root.getChildren().addAll(gameLayer, hud, topControls, bottomControls, backButton);

    return root;
  }

  /**
   * Adds an overlay node (e.g. a HUD) on top of the game layer at the given alignment within the
   * root StackPane. Idempotent: a previously attached node at the same identity is removed first to
   * support fresh sessions on level restart.
   */
  public void attachOverlay(Node overlay, Pos alignment) {
    StackPane root = (StackPane) getRoot();
    root.getChildren().remove(overlay);
    StackPane.setAlignment(overlay, alignment);
    root.getChildren().add(overlay);
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_VEHICLE_BUILDER));

    return backButton;
  }

  private Pane createGameAreaRegion() {
    Pane gameContainer = new Pane();
    gameContainer.getStyleClass().add("game-container");
    return gameContainer;
  }

  private HBox createTopControlsRegion() {
    HBox container = new HBox(d("top.controls.spacing"));
    container.getStyleClass().add("controlbuttons-top");

    container.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
    container.setPickOnBounds(false);

    ToggleButton pauseBtn = new ToggleButton(s("button.pause"));
    pauseBtn.setOnAction(e -> {
      if (pauseBtn.isSelected()) {
        onPause.run();
        pauseBtn.setText(s("button.resume"));
      } else {
        onResume.run();
        pauseBtn.setText(s("button.pause"));
      }
    });
    pauseBtn.getStyleClass().add("sys-button");

    Button resetBtn = new Button(s("button.reset"));
    resetBtn.setOnAction(e -> onReset.run());
    resetBtn.getStyleClass().add("sys-button");

    Button tempWinBtn = new Button(s("button.debug.win"));
    tempWinBtn.getStyleClass().add("sys-button");
    tempWinBtn.setOnAction(event -> onOutcome.accept(Outcome.GAME_WON));

    Button tempLoseBtn = new Button(s("button.debug.lose"));
    tempLoseBtn.getStyleClass().add("sys-button");
    tempLoseBtn.setOnAction(event -> onOutcome.accept(Outcome.GAME_LOST));

    container.getChildren().addAll(pauseBtn, resetBtn, tempWinBtn, tempLoseBtn);
    return container;
  }

  // availableToolIds == null means "show every tool in the properties file" (startup default).
  private HBox createBottomControlsRegion(Set<String> availableToolIds) {
    HBox container = new HBox(d("bottom.controls.spacing"));
    container.getStyleClass().add("controlbuttons-bottom");

    container.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
    container.setPickOnBounds(false);

    for (String tool : getList("tools")) {
      if (availableToolIds != null && !availableToolIds.contains(tool)) {
        continue;
      }
      ToggleButton toolBtn = new ToggleButton(s("tool." + tool));
      toolBtn.getStyleClass().add("tool-button");

      toolBtn.setOnAction(e -> {
        if (toolBtn.isSelected()) {
          onToolActivated.accept(tool);
        } else {
          onToolDeactivated.accept(tool);
        }
      });

      container.getChildren().add(toolBtn);
    }

    return container;
  }

  public void setAvailableTools(Collection<String> toolIds) {
    if (bottomControls == null) {
      return;
    }
    Set<String> filter = toolIds == null
        ? null
        : toolIds.stream().map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));
    HBox rebuilt = createBottomControlsRegion(filter);
    bottomControls.getChildren().setAll(rebuilt.getChildren());
  }

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
