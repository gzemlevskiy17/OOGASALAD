package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.ScoreCalculator;
import org.junit.jupiter.api.Test;

class ScoreCalculatorImplTest {

  private final ScoreCalculator calculator = new ScoreCalculatorImpl();

  private GameplayScore run(double distance, double remaining, double span,
      double progress, boolean won) {
    return new GameplayScore(
        "level", distance, remaining, span, progress, 0.0, won, Instant.EPOCH);
  }

  @Test
  void winOnPerfectStraightLine_scoresFullPoints() {
    // span=1000, traveled exactly 1000, ended at finish → efficiency = 1.0
    GameplayScore winRun = run(1000, 0, 1000, 1.0, true);

    assertEquals(100.0, calculator.score(winRun), 1e-6);
  }

  @Test
  void winWithWanderingPath_keepsFullProgressButPaysOnEfficiency() {
    // Won, but traveled twice the necessary distance → efficiency = 0.5
    GameplayScore wanderingWin = run(2000, 0, 1000, 1.0, true);

    // 70 * 1.0 (progress forced to 1 on win) + 30 * 0.5 = 85
    assertEquals(85.0, calculator.score(wanderingWin), 1e-6);
  }

  @Test
  void lossWithStraightHalfProgress_scoresProgressPlusFullEfficiency() {
    // Died halfway, went straight: distance == displacement → efficiency = 1.0
    GameplayScore halfwayLoss = run(500, 500, 1000, 0.5, false);

    // 70 * 0.5 + 30 * 1.0 = 65
    assertEquals(65.0, calculator.score(halfwayLoss), 1e-6);
  }

  @Test
  void runWithNoMovement_scoresZero() {
    GameplayScore stillborn = run(0, 1000, 1000, 0.0, false);

    assertEquals(0.0, calculator.score(stillborn), 1e-6);
  }

  @Test
  void winAlwaysScoresAtLeastTheProgressWeight_evenWithTerribleEfficiency() {
    // Took a million units of path to cover a 1000-unit level
    GameplayScore epicWanderingWin = run(1_000_000, 0, 1000, 1.0, true);

    assertTrue(calculator.score(epicWanderingWin) >= 70.0);
  }
}
