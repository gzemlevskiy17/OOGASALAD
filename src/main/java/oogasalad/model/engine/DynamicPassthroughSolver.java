package oogasalad.model.engine;

import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.PickupComponent;

/**
 * The solver for the Dynamic - PassThrough Collision.
 *
 * <p>Pass-through bodies do not produce a physical response. When the incident body is a {@link
 * PickupComponent}, this solver delegates to the pickup so its effect fires and the pickup is
 * consumed; otherwise the call is a no-op.
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
    if (incidentObject instanceof PickupComponent pickup) {
      pickup.applyTo(referenceObject);
    }
  }
}
