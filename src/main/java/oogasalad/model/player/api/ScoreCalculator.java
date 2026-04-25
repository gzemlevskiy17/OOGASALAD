package oogasalad.model.player.api;

/** Computes a numeric score from a finished gameplay run. */
public interface ScoreCalculator {

  /** Returns the score for the given run. */
  double score(GameplayScore run);
}
