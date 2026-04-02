package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.core.internal.AbstractAppScreen;

public class GamePlayAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");

  public GamePlayAppScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected StackPane createRoot() {
    StackPane root = new StackPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/gameplay.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    Pane gameArea = createGameAreaRegion();

    HBox topControls = createTopControlsRegion();
    HBox bottomControls = createBottomControlsRegion();

    StackPane.setAlignment(topControls, Pos.TOP_LEFT);
    StackPane.setAlignment(bottomControls, Pos.BOTTOM_CENTER);

    root.getChildren().addAll(gameArea, topControls, bottomControls);

    return root;
  }

  private Pane createGameAreaRegion() {
    // This Pane will hold the rendering
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
    pauseBtn.getStyleClass().add("sys-button");

    Button resetBtn = new Button(s("button.reset"));
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
      Button toolBtn = new Button(s("tool." + tool));
      toolBtn.getStyleClass().add("tool-button");

      //TODO: add toggle logic
      toolBtn.setOnAction(e -> System.out.println("Selected tool: " + tool));

      container.getChildren().add(toolBtn);
    }

    return container;
  }

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}