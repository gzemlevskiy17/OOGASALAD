package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * In-game HUD shown at the top-center of the gameplay screen. Two rows: the current elapsed
 * time as formatted text, and a progress bar bound to progress percent in {@code [0.0, 1.0]}.
 * <p>
 * Call {@link #update(double, double)} each simulation tick with the latest stats from
 * {@code GameplayStatsTracker}. Values outside {@code [0.0, 1.0]} are clamped for the bar.
 * The component is mouse-transparent so it does not intercept clicks on the game layer.
 *
 * @author Cody
 */
public class GameHudDisplay extends VBox {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.player.GamePlayAppScreen");

  private final Label timeLabel;
  private final ProgressBar progressBar;

  public GameHudDisplay() {
    this.timeLabel = new Label();
    this.timeLabel.getStyleClass().add("hud-time-label");

    this.progressBar = new ProgressBar(0.0);
    this.progressBar.getStyleClass().add("hud-progress-bar");
    this.progressBar.setPrefWidth(d("hud.progress.width"));

    getStyleClass().add("hud-display");
    setAlignment(Pos.TOP_CENTER);
    setSpacing(d("hud.spacing"));
    setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    setMouseTransparent(true);

    getChildren().addAll(timeLabel, progressBar);
    update(0.0, 0.0);
  }

  /**
   * Updates both rows with the latest stats. Invoked from the controller's stats-tick callback
   * on the JavaFX application thread.
   *
   * @param elapsedSeconds non-negative seconds since session start
   * @param progressPercent progress toward the goal; clamped to {@code [0.0, 1.0]}
   */
  public void update(double elapsedSeconds, double progressPercent) {
    long total = (long) Math.max(0.0, Math.floor(elapsedSeconds));
    long minutes = total / 60;
    long seconds = total % 60;
    timeLabel.setText(String.format(PROPS.getString("hud.time.format"), minutes, seconds));
    progressBar.setProgress(clamp01(progressPercent));
  }

  private static double clamp01(double v) {
    if (Double.isNaN(v) || v < 0.0) {
      return 0.0;
    }
    return Math.min(v, 1.0);
  }

  private static double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
