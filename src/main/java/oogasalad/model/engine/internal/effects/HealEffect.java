package oogasalad.model.engine.internal.effects;

import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.PickupEffect;

/**
 * A {@link PickupEffect} that restores HP to a {@link Damageable} target. The repair amount is
 * clamped at the target's max HP by {@link Damageable#repair(double)}; targets that do not
 * implement {@code Damageable} are unaffected (the pickup is still consumed by the framework).
 *
 * @author Paulina Vvedenskaya
 */
public final class HealEffect implements PickupEffect {

  private final double amount;

  public HealEffect(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("heal amount must be non-negative");
    }
    this.amount = amount;
  }

  @Override
  public void apply(GameObject target) {
    if (target instanceof Damageable d) {
      d.repair(amount);
    }
  }
}
