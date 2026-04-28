package oogasalad.model.engine.api;

/**
 * Marker interface for components that carry a {@link CollisionEffect} to fire when a dynamic body
 * touches them. Solvers check for this capability instead of branching on concrete types.
 *
 * @author Paulina Vvedenskaya
 */
public interface HasCollisionEffect {

  /**
   * @return the effect to apply on contact, or {@code null} if the component is currently inert.
   */
  CollisionEffect getCollisionEffect();
}
