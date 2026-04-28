package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.HasCollisionEffect;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;

/**
 * The solver for the Dynamic - PassThrough Collision.
 *
 * <p>Pass-through bodies do not produce a physical response. When the incident body carries a
 * {@link CollisionEffect} (i.e. implements {@link HasCollisionEffect}), this solver invokes it on
 * the first iteration only; otherwise the call is a no-op.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public class DynamicPassthroughSolver {

  /**
   * Resolves one collision without applying physical response.
   *
   * @param referenceObject dynamic object in the collision
   * @param incidentObject incident pass-through object in the collision
   * @param collision collision data representing the collision between the two
   * @param isFirstIteration true only on the first solver iteration for the collision set
   * @param solveImpulse ignored by this solver
   */
  public void solve(DynamicComponent referenceObject, PassThroughComponent incidentObject,
      Collision collision, boolean isFirstIteration, boolean solveImpulse) {
    if (isFirstIteration && incidentObject instanceof HasCollisionEffect actor) {
      CollisionEffect effect = actor.getCollisionEffect();
      if (effect != null) {
        effect.apply(referenceObject);
      }
    }
  }
}
