package oogasalad.view.player.internal;

import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;
import oogasalad.view.core.internal.TerrainPaints;
import oogasalad.view.player.api.GameplayView;

/**
 * Screen for the main window of the gameplay
 * @author Iurii Beliaev
 */

public class GamePlayAppScreen extends AbstractAppScreen implements GameplayView {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");

  private Pane gameLayer;
  private ImageView backgroundImageView;
  private String backgroundImagePath = "";
  private HBox bottomControls;
  private GameHudDisplay hud;
  private Runnable onPause = () -> { };
  private Runnable onResume = () -> { };
  private Runnable onReset = () -> { };
  private Consumer<KeyCode> onKeyPressed = key -> { };
  private Consumer<KeyCode> onKeyReleased = key -> { };
  private java.util.function.Consumer<String> onToolActivated = id -> {
  };
  private java.util.function.Consumer<String> onToolDeactivated = id -> {
  };

  public GamePlayAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  @Override
  public Pane getGameLayer() {
    return gameLayer;
  }

  public void setBackgroundImagePath(String imagePath) {
    backgroundImagePath = imagePath == null ? "" : imagePath.strip();
    if (gameLayer == null) {
      return;
    }
    if (backgroundImagePath.isBlank()) {
      removeBackgroundImage();
      return;
    }
    Image image = TerrainPaints.loadImage(backgroundImagePath);
    if (image == null || image.isError()) {
      removeBackgroundImage();
      return;
    }
    ensureBackgroundImageView();
    backgroundImageView.setImage(image);
    backgroundImageView.toBack();
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

  public void updateHud(double elapsedSeconds, int score, double progressPercent) {
    if (hud != null) {
      hud.update(elapsedSeconds, score, progressPercent);
    }
  }

  @Override
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

  @Override
  public void setOnReset(Runnable onReset) {
    if (onReset == null) {
      this.onReset = () -> { };
    } else {
      this.onReset = onReset;
    }
  }

  public void setOnKeyPressed(Consumer<KeyCode> onKeyPressed) {
    this.onKeyPressed = onKeyPressed;
  }

  public void setOnKeyReleased(Consumer<KeyCode> onKeyReleased) {
    this.onKeyReleased = onKeyReleased;
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
    wireKeyboardInput(root);
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/gameplay.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    gameLayer = createGameAreaRegion();
    gameLayer.prefWidthProperty().bind(root.widthProperty());
    gameLayer.prefHeightProperty().bind(root.heightProperty());

    HBox topControls = createTopControlsRegion();
    bottomControls = new HBox(d("bottom.controls.spacing"));
    bottomControls.getStyleClass().add("controlbuttons-bottom");
    bottomControls.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE,
        javafx.scene.layout.Region.USE_PREF_SIZE);
    bottomControls.setPickOnBounds(false);
    hud = new GameHudDisplay();

    Node backButton = createBackButtonRegion();

    StackPane.setAlignment(topControls, Pos.TOP_LEFT);
    StackPane.setAlignment(hud, Pos.TOP_CENTER);
    StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
    StackPane.setAlignment(bottomControls, Pos.BOTTOM_CENTER);

    root.getChildren().addAll(gameLayer, hud, topControls, bottomControls, backButton);

    return root;
  }

  public void requestKeyboardFocus() {
    getRoot().requestFocus();
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
    if (!backgroundImagePath.isBlank()) {
      gameLayer = gameContainer;
      setBackgroundImagePath(backgroundImagePath);
    }
    return gameContainer;
  }

  private void ensureBackgroundImageView() {
    if (backgroundImageView == null) {
      backgroundImageView = new ImageView();
      backgroundImageView.setMouseTransparent(true);
      backgroundImageView.setPreserveRatio(false);
      backgroundImageView.fitWidthProperty().bind(gameLayer.widthProperty());
      backgroundImageView.fitHeightProperty().bind(gameLayer.heightProperty());
    }
    if (!gameLayer.getChildren().contains(backgroundImageView)) {
      gameLayer.getChildren().add(0, backgroundImageView);
    }
  }

  private void removeBackgroundImage() {
    if (backgroundImageView != null && gameLayer != null) {
      gameLayer.getChildren().remove(backgroundImageView);
      backgroundImageView.setImage(null);
    }
  }

  private void wireKeyboardInput(StackPane root) {
    root.setFocusTraversable(true);
    root.addEventHandler(KeyEvent.KEY_PRESSED,
        event -> onKeyPressed.accept(event.getCode()));
    root.addEventHandler(KeyEvent.KEY_RELEASED,
        event -> onKeyReleased.accept(event.getCode()));
    root.setOnMouseClicked(event -> root.requestFocus());
    root.sceneProperty().addListener((observable, oldScene, newScene) -> {
      if (newScene != null) {
        Platform.runLater(root::requestFocus);
      }
    });
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

    container.getChildren().addAll(pauseBtn, resetBtn);
    return container;
  }

  public void setAvailableTools(Collection<String> toolIds) {
    if (bottomControls == null) {
      return;
    }
    bottomControls.getChildren().clear();
    if (toolIds == null || toolIds.isEmpty()) {
      return;
    }
    for (String toolId : toolIds) {
      String label = toolLabel(toolId);
      ToggleButton toolBtn = new ToggleButton(label);
      toolBtn.getStyleClass().add("tool-button");
      toolBtn.setOnAction(e -> {
        if (toolBtn.isSelected()) {
          onToolActivated.accept(toolId);
        } else {
          onToolDeactivated.accept(toolId);
        }
      });
      bottomControls.getChildren().add(toolBtn);
    }
  }

  private String toolLabel(String toolId) {
    String key = "tool." + toolId;
    try {
      return PROPS.getString(key);
    } catch (java.util.MissingResourceException e) {
      if (toolId.isEmpty()) {
        return toolId;
      }
      return Character.toUpperCase(toolId.charAt(0)) + toolId.substring(1);
    }
  }

  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
