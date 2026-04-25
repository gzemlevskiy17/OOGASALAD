package oogasalad.model.engine.api;

/**
 * Hook for static obstacles that damage dynamic bodies on contact (rocks, spikes, lava, etc.).
 * Implementations encapsulate their own damage amount and cooldown so the collision solver stays
 * generic — it just delegates each contact to {@link #damageContact(Damageable)} and lets the
 * obstacle decide whether to actually apply damage on this frame.
 *
 * @author Paulina Vvedenskaya
 */
public interface ContactDamager {

  void damageContact(Damageable target);
}
