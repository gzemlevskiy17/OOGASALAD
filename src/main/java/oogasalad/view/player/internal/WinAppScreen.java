package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen displayed when the player wins the level.
 */
public class WinAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.player.WinAppScreen");

  private Label timeLabel;
  private Label progressLabel;

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

    VBox statsBox = buildStatsBox();

    HBox buttons = new HBox(16);
    buttons.setAlignment(Pos.CENTER);

    Button continueButton = new Button(s("continue.button.text"));
    continueButton.getStyleClass().add("result-button");
    continueButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_VEHICLE_BUILDER));

    Button playSomethingButton = new Button(s("play.something.button.text"));
    playSomethingButton.getStyleClass().add("result-button");
    playSomethingButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_PLAYER_ENTRY));

    Button homeButton = new Button(s("home.button.text"));
    homeButton.getStyleClass().add("result-button");
    homeButton.setOnAction(event -> onOutcome.accept(Outcome.BACK_TO_MENU));

    buttons.getChildren().addAll(continueButton, playSomethingButton, homeButton);
    content.getChildren().addAll(title, statsBox, buttons);
    root.setCenter(content);
    return root;
  }

  private VBox buildStatsBox() {
    VBox box = new VBox();
    box.getStyleClass().add("result-stats");
    box.setAlignment(Pos.CENTER_LEFT);
    box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    timeLabel = new Label();
    timeLabel.getStyleClass().add("result-stats-label");

    progressLabel = new Label();
    progressLabel.getStyleClass().add("result-stats-label");

    box.getChildren().addAll(timeLabel, progressLabel);
    setStats(0.0, 1.0);
    return box;
  }

  /**
   * Updates the elapsed time and progress labels shown in the centered stats box.
   *
   * @param elapsedSeconds seconds from session start at the moment of winning (>= 0)
   * @param progressPercent progress toward the goal in [0.0, 1.0]; typically 1.0 on a win
   */
  public void setStats(double elapsedSeconds, double progressPercent) {
    long total = (long) Math.max(0.0, Math.floor(elapsedSeconds));
    long minutes = total / 60;
    long seconds = total % 60;
    int percent = (int) Math.round(clamp01(progressPercent) * 100.0);
    timeLabel.setText(String.format(s("stats.time.format"), minutes, seconds));
    progressLabel.setText(String.format(s("stats.progress.format"), percent));
  }

  private static double clamp01(double v) {
    if (Double.isNaN(v) || v < 0.0) {
      return 0.0;
    }
    return Math.min(v, 1.0);
  }

  private String s(String key) {
    return PROPS.getString(key);
  }
}
