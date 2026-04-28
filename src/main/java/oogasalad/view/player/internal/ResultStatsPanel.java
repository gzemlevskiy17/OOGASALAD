package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shared three-row stats panel used by Win and Lose result screens. Each row is a main label
 * plus an optional green "New Record!" badge shown when the current run beats the user's prior
 * best on that axis. Reads label formats from the passed resource bundle ({@code stats.time.format},
 * {@code stats.score.format}, {@code stats.progress.format}, {@code stats.new.record.text}).
 */
class ResultStatsPanel {

  private final ResourceBundle props;
  private final VBox box;
  private final Label timeValue;
  private final Label timeBadge;
  private final Label scoreValue;
  private final Label scoreBadge;
  private final Label progressValue;
  private final Label progressBadge;

  ResultStatsPanel(ResourceBundle props) {
    this.props = props;
    this.box = new VBox();
    this.box.getStyleClass().add("result-stats");
    this.box.setAlignment(Pos.CENTER_LEFT);
    this.box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    this.timeValue = newValueLabel();
    this.timeBadge = newBadgeLabel();
    this.scoreValue = newValueLabel();
    this.scoreBadge = newBadgeLabel();
    this.progressValue = newValueLabel();
    this.progressBadge = newBadgeLabel();

    box.getChildren().addAll(
        row(timeValue, timeBadge),
        row(scoreValue, scoreBadge),
        row(progressValue, progressBadge));

    setStats(0.0, 0.0, 0.0, false, false, false);
  }

  VBox getNode() {
    return box;
  }

  void setStats(double elapsedSeconds, double score, double progressPercent,
      boolean isNewTime, boolean isNewScore, boolean isNewProgress) {
    long total = (long) Math.max(0.0, Math.floor(elapsedSeconds));
    long minutes = total / 60;
    long seconds = total % 60;
    int scoreInt = (int) Math.round(clamp(score, 0.0, 100.0));
    int progressInt = (int) Math.round(clamp01(progressPercent) * 100.0);

    timeValue.setText(String.format(props.getString("stats.time.format"), minutes, seconds));
    scoreValue.setText(String.format(props.getString("stats.score.format"), scoreInt));
    progressValue.setText(String.format(props.getString("stats.progress.format"), progressInt));

    String newRecordText = props.getString("stats.new.record.text");
    applyBadge(timeBadge, isNewTime, newRecordText);
    applyBadge(scoreBadge, isNewScore, newRecordText);
    applyBadge(progressBadge, isNewProgress, newRecordText);
  }

  private HBox row(Label value, Label badge) {
    HBox r = new HBox(12, value, badge);
    r.setAlignment(Pos.CENTER_LEFT);
    return r;
  }

  private Label newValueLabel() {
    Label l = new Label();
    l.getStyleClass().add("result-stats-label");
    return l;
  }

  private Label newBadgeLabel() {
    Label l = new Label();
    l.getStyleClass().add("result-stats-badge");
    l.setVisible(false);
    l.setManaged(false);
    return l;
  }

  private static void applyBadge(Label badge, boolean show, String text) {
    badge.setText(text);
    badge.setVisible(show);
    badge.setManaged(show);
  }

  private static double clamp(double v, double min, double max) {
    if (Double.isNaN(v) || v < min) {
      return min;
    }
    return Math.min(v, max);
  }

  private static double clamp01(double v) {
    return clamp(v, 0.0, 1.0);
  }
}
