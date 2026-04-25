package oogasalad.model.engine.api;

import oogasalad.model.engine.EngineError;

/**
 * Contract for objects that can take and recover damage. Implementations must store the four
 * primitive HP values; the {@link #damage}, {@link #repair}, and {@link #isDestroyed} defaults
 * derive from those.
 *
 * <p>Implementations decide what to do at zero HP — for example, vehicle parts may deactivate
 * themselves while pigs only signal a loss condition.
 *
 * @author Paulina Vvedenskaya
 */
public interface Damageable {

  double getCurrentHp();

  void setCurrentHp(double hp);

  double getMaxHp();

  void setMaxHp(double maxHp);

  default void damage(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException(EngineError.DAMAGE_MUST_BE_NON_NEGATIVE.message());
    }
    setCurrentHp(Math.max(0, getCurrentHp() - amount));
  }

  default void repair(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException(EngineError.REPAIR_MUST_BE_NON_NEGATIVE.message());
    }
    setCurrentHp(Math.min(getMaxHp(), getCurrentHp() + amount));
  }

  default boolean isDestroyed() {
    return getCurrentHp() <= 0;
  }
}
