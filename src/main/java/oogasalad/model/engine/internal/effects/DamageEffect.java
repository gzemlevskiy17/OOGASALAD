package oogasalad.model.engine.internal.effects;

import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.PickupEffect;

/**
 * A {@link PickupEffect} that subtracts HP from a {@link Damageable} target. The damage amount is
 * clamped at zero by {@link Damageable#damage(double)}; targets that do not implement
 * {@code Damageable} are unaffected (the pickup is still consumed by the framework).
 *
 * @author Paulina Vvedenskaya
 */
public final class DamageEffect implements PickupEffect {

  private final double amount;

  public DamageEffect(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("damage amount must be non-negative");
    }
    this.amount = amount;
  }

  @Override
  public void apply(GameObject target) {
    if (target instanceof Damageable d) {
      d.damage(amount);
    }
  }
}
