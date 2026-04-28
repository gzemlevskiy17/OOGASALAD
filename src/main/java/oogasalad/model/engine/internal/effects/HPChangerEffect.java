package oogasalad.model.engine.internal.effects;

import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.GameObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adjusts a {@link Damageable} target's HP by a fixed amount on contact. Positive amounts heal,
 * negative amounts damage. Components that aren't {@code Damageable} are ignored.
 *
 * @author Paulina Vvedenskaya
 */
public final class HPChangerEffect implements CollisionEffect {

  private static final Logger LOG = LogManager.getLogger(HPChangerEffect.class);

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
    LOG.debug("{} {} HP applied to {} (now {}/{})",
        amount >= 0 ? "Heal" : "Damage", Math.abs(amount),
        target.getClass().getSimpleName(), damageable.getCurrentHp(), damageable.getMaxHp());
  }
}
