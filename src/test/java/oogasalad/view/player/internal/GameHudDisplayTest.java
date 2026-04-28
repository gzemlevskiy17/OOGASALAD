package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

class GameHudDisplayTest extends DukeApplicationTest {

  private GameHudDisplay hud;

  @Override
  public void start(Stage stage) {
    hud = new GameHudDisplay();
    stage.setScene(new Scene(new StackPane(hud), 400, 200));
    stage.show();
  }

  private Label scoreLabel() {
    return (Label) hud.getChildren().get(0);
  }

  private Label timeLabel() {
    return (Label) hud.getChildren().get(1);
  }

  @Test
  void constructor_defaultsScoreToZeroOutOfHundred() {
    assertEquals("Score: 0 / 100", scoreLabel().getText());
  }

  @Test
  void scoreLabel_isFirstChildAboveTimeLabel() {
    assertSame(scoreLabel(), hud.getChildren().get(0));
    assertSame(timeLabel(), hud.getChildren().get(1));
    assertTrue(hud.getChildren().get(2) instanceof ProgressBar);
  }

  @Test
  void scoreLabel_hasScoreStyleClass() {
    assertTrue(scoreLabel().getStyleClass().contains("hud-score-label"));
  }

  @Test
  void updateScore_setsFormattedText() {
    runAsJFXAction(() -> hud.updateScore(42));
    assertEquals("Score: 42 / 100", scoreLabel().getText());
  }

  @Test
  void updateScore_clampsNegativeToZero() {
    runAsJFXAction(() -> hud.updateScore(-15));
    assertEquals("Score: 0 / 100", scoreLabel().getText());
  }

  @Test
  void updateScore_clampsAboveHundredToHundred() {
    runAsJFXAction(() -> hud.updateScore(250));
    assertEquals("Score: 100 / 100", scoreLabel().getText());
  }

  @Test
  void updateScore_acceptsBoundaryValues() {
    runAsJFXAction(() -> hud.updateScore(0));
    assertEquals("Score: 0 / 100", scoreLabel().getText());

    runAsJFXAction(() -> hud.updateScore(100));
    assertEquals("Score: 100 / 100", scoreLabel().getText());
  }

  @Test
  void updateOverload_updatesTimeScoreAndProgressTogether() {
    runAsJFXAction(() -> hud.update(75.0, 37, 0.5));
    assertEquals("Score: 37 / 100", scoreLabel().getText());
    assertEquals("Time: 1:15", timeLabel().getText());
    ProgressBar bar = (ProgressBar) hud.getChildren().get(2);
    assertEquals(0.5, bar.getProgress(), 1e-9);
  }

  @Test
  void timeOnlyUpdate_doesNotChangeScore() {
    runAsJFXAction(() -> hud.updateScore(80));
    runAsJFXAction(() -> hud.update(10.0, 0.25));
    assertEquals("Score: 80 / 100", scoreLabel().getText());
  }
}
