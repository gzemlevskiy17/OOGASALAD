package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.api.AbstractAppScreen;

/**
 * Screen displayed when the player wins the level.
 */
public class WinAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.player.WinAppScreen");

  private ResultStatsPanel statsPanel;

  public WinAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  @Override
  protected Parent createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(
        getClass().getResource("/oogasalad/view/player/result-screen.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    VBox content = new VBox(24);
    content.getStyleClass().add("result-card");
    content.setAlignment(Pos.CENTER);

    Label title = new Label(s("title.text"));
    title.getStyleClass().add("result-title");

    statsPanel = new ResultStatsPanel(PROPS);

    HBox buttons = new HBox(16);
    buttons.setAlignment(Pos.CENTER);

    Button continueButton = new Button(s("continue.button.text"));
    continueButton.getStyleClass().add("result-button");
    continueButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_NEXT_LEVEL));

    Button playSomethingButton = new Button(s("play.something.button.text"));
    playSomethingButton.getStyleClass().add("result-button");
    playSomethingButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_PLAYER_ENTRY));

    Button homeButton = new Button(s("home.button.text"));
    homeButton.getStyleClass().add("result-button");
    homeButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_MENU));

    buttons.getChildren().addAll(continueButton, playSomethingButton, homeButton);
    content.getChildren().addAll(title, statsPanel.getNode(), buttons);
    root.setCenter(content);
    return root;
  }

  /**
   * Updates the three stat rows. Each {@code isNewRecord*} flag toggles a green badge next to
   * its row; pass true for all three when this is the user's first run on the level.
   */
  public void setStats(double elapsedSeconds, double score, double progressPercent,
      boolean isNewTime, boolean isNewScore, boolean isNewProgress) {
    statsPanel.setStats(elapsedSeconds, score, progressPercent,
        isNewTime, isNewScore, isNewProgress);
  }

  private String s(String key) {
    return PROPS.getString(key);
  }
}
