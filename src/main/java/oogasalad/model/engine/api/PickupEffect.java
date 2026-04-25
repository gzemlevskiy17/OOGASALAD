package oogasalad.model.engine.api;

/**
 * One-shot behavior applied when an object picks up a pickup. Concrete effects (heal, damage, award
 * score, apply buff) capture their dependencies at construction so this contract stays narrow.
 * Implementations decide how to interpret the target — for example, downcasting to
 * {@link Damageable} when modifying health.
 *
 * @author Paulina Vvedenskaya
 */
@FunctionalInterface
public interface PickupEffect {

  void apply(GameObject target);
}
