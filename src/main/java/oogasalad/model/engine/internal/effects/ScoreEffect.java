package oogasalad.model.engine.internal.effects;

import java.util.Objects;
import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.player.api.ScoreService;

/**
 * A {@link CollisionEffect} that awards points to a {@link ScoreService}. Used by both coin and XP
 * rewards — distinguished only by the configured {@code amount}. The target is irrelevant to a
 * score award, so it is ignored.
 *
 * @author Paulina Vvedenskaya
 */
public final class ScoreEffect implements CollisionEffect {

  private final ScoreService score;
  private final int amount;

  public ScoreEffect(ScoreService score, int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("score amount must be non-negative");
    }
    this.score = Objects.requireNonNull(score);
    this.amount = amount;
  }

  @Override
  public void apply(GameObject target) {
    score.addPoints(amount);
  }
}
