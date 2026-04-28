package oogasalad.model.player.internal;

import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.ScoreCalculator;

/**
 * Weighted sum of effective progress and path efficiency.
 *
 * <p>Effective progress is forced to 1.0 on a win (so a winning run always earns full progress
 * credit even if the pig wasn't dead-on the goal Y); on a loss it falls back to the run's
 * {@link GameplayScore#progressPercent()}. Efficiency is the ratio of straight-line displacement
 * achieved to actual path length traveled, clamped to {@code [0, 1]} — 1.0 means a perfectly
 * direct path, lower values mean wandering or backtracking. A run with no movement scores 0.
 */
public class ScoreCalculatorImpl implements ScoreCalculator {

  private static final double PROGRESS_WEIGHT = 70.0;
  private static final double EFFICIENCY_WEIGHT = 30.0;

  @Override
  public double score(GameplayScore run) {
    double effectiveProgress = run.won() ? 1.0 : run.progressPercent();
    double displacement = run.levelSpan() - run.remainingDistance();
    double efficiency = run.distanceTraveled() > 0
        ? Math.max(0.0, Math.min(1.0, displacement / run.distanceTraveled()))
        : 0.0;
    return PROGRESS_WEIGHT * effectiveProgress + EFFICIENCY_WEIGHT * efficiency;
  }
}
