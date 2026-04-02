package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.core.internal.AbstractAppScreen;

public class GamePlayAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.GamePlayScreen");

  public GamePlayAppScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/gameplay.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    return root;
  }
}
