package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.HasCollisionEffect;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;

/**
 * The solver for the Dynamic - PassThrough Collision.
 *
 * <p>Pass-through bodies do not produce a physical response. When the incident body carries a
 * {@link CollisionEffect} (i.e. implements {@link HasCollisionEffect}), this solver invokes it
 * against the dynamic body; otherwise the call is a no-op.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public class DynamicPassthroughSolver {

  /**
   * Constructor for the DynamicPassthroughSolver.
   *
   * @param referenceObject - dynamic object in the collision
   * @param incidentObject  - incident object in the collision
   * @param collision       - Collision type object representing the collision between the two
   */
  public void solve(DynamicComponent referenceObject, PassThroughComponent incidentObject,
      Collision collision) {
    if (incidentObject instanceof HasCollisionEffect actor) {
      CollisionEffect effect = actor.getCollisionEffect();
      if (effect != null) {
        effect.apply(referenceObject);
      }
    }
  }
}
