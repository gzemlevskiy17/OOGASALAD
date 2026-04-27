package oogasalad.model.engine.internal.effects;

import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.GameObject;

/**
 * Adjusts a {@link Damageable} target's HP by a fixed amount on contact. Positive amounts heal,
 * negative amounts damage. Components that aren't {@code Damageable} are ignored.
 *
 * @author Paulina Vvedenskaya
 */
public final class HPChangerEffect implements CollisionEffect {

  private final double amount;

  public HPChangerEffect(double amount) {
    this.amount = amount;
  }

  @Override
  public void apply(GameObject target) {
    if (!(target instanceof Damageable damageable)) {
      return;
    }
    if (amount >= 0) {
      damageable.repair(amount);
    } else {
      damageable.damage(-amount);
    }
  }
}
