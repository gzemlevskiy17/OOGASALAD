package oogasalad.model.player.internal;

import oogasalad.model.player.api.ScoreService;

/**
 * In-memory {@link ScoreService} backed by a single int. Constructed once per gameplay session.
 * Negative {@code addPoints} arguments are rejected so penalty mechanics are an explicit future
 * decision rather than an implicit possibility.
 *
 * @author Paulina Vvedenskaya
 */
public final class ScoreServiceImpl implements ScoreService {

  private int points;

  public ScoreServiceImpl() {
    this.points = 0;
  }

  @Override
  public int getPoints() {
    return points;
  }

  @Override
  public void addPoints(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("score amount must be non-negative");
    }
    this.points += amount;
  }
}
