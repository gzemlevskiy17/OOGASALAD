package oogasalad.model.engine.api;

/**
 * Behavior applied to a dynamic body when it touches a collision-active static or pass-through
 * component (pickup, damaging obstacle, finish trigger, etc.). Concrete effects (heal, damage,
 * award score, apply buff) capture their dependencies at construction so this contract stays
 * narrow. Implementations decide how to interpret the target — for example, downcasting to
 * {@link Damageable} when modifying health.
 *
 * @author Paulina Vvedenskaya
 */
@FunctionalInterface
public interface CollisionEffect {

  void apply(GameObject target);
}
