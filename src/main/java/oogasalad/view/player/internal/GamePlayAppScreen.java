package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen for the main window of the gameplay
 * @author Iurii Beliaev
 */

public class GamePlayAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");

  private Pane gameLayer;
  private Runnable onPause = () -> { };
  private Runnable onReset = () -> { };

  public GamePlayAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  public Pane getGameLayer() {
    return gameLayer;
  }

  public void setOnPause(Runnable onPause) {
    if (onPause == null) {
      this.onPause = () -> { };
    } else {
      this.onPause = onPause;
    }
  }

  public void setOnReset(Runnable onReset) {
    if (onReset == null) {
      this.onReset = () -> { };
    } else {
      this.onReset = onReset;
    }
  }

  @Override
  protected StackPane createRoot() {
    StackPane root = new StackPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/gameplay.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    gameLayer = createGameAreaRegion();

    HBox topControls = createTopControlsRegion();
    HBox bottomControls = createBottomControlsRegion();

    Node backButton = createBackButtonRegion();

    StackPane.setAlignment(topControls, Pos.TOP_LEFT);
    StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
    StackPane.setAlignment(bottomControls, Pos.BOTTOM_CENTER);

    root.getChildren().addAll(gameLayer, topControls, bottomControls, backButton);

    return root;
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
    container.getStyleClass().add("hud-top");

    container.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
    container.setPickOnBounds(false);

    Button pauseBtn = new Button(s("button.pause"));
    pauseBtn.setOnAction(e -> onPause.run());
    pauseBtn.getStyleClass().add("sys-button");

    Button resetBtn = new Button(s("button.reset"));
    resetBtn.setOnAction(e -> onReset.run());
    resetBtn.getStyleClass().add("sys-button");

    container.getChildren().addAll(pauseBtn, resetBtn);
    return container;
  }

  private HBox createBottomControlsRegion() {
    HBox container = new HBox(d("bottom.controls.spacing"));
    container.getStyleClass().add("hud-bottom");

    container.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
    container.setPickOnBounds(false);

    for (String tool : getList("tools")) {
      ToggleButton toolBtn = new ToggleButton(s("tool." + tool));
      toolBtn.getStyleClass().add("tool-button");

      toolBtn.setOnAction(e -> {
        // TODO: Update GameController
        System.out.println(tool + " is now " + (toolBtn.isSelected() ? "Active" : "Inactive"));
      });

      container.getChildren().add(toolBtn);
    }

    return container;
  }

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
