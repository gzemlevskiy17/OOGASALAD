package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen for the main window of the gameplay
 * @author Iurii Beliaev
 */

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

    Node backButton = createBackButtonRegion();

    StackPane.setAlignment(topControls, Pos.TOP_LEFT);
    StackPane.setAlignment(backButton, Pos.TOP_RIGHT);
    StackPane.setAlignment(bottomControls, Pos.BOTTOM_CENTER);

    root.getChildren().addAll(gameArea, topControls, bottomControls, backButton);

    return root;
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> screenManager.switchScreen(ScreenFactory.VEHICLE_BUILDER_APP_SCREEN_KEY, null));

    return backButton;
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
      ToggleButton toolBtn = new ToggleButton(s("tool." + tool));
      toolBtn.getStyleClass().add("tool-button");

      // You only need the listener if you want to update your game model.
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