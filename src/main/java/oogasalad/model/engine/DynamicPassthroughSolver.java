package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;

/**
 * The solver for the Dynamic - PassThrough Collision
 *
 * @author Iurii Beliaev
 */
public class DynamicPassthroughSolver {

  private DynamicComponent referenceObject;
  private PassThroughComponent incidentObject;

  /**
   * Constructor for the DynamicPassthroughSolver
   *
   * @param referenceObject - dynamic object in the collision
   * @param incidentObject - insident object in the collision
   * @param collision - Collision type object representing the collision between the two
   */
  public void solve(DynamicComponent referenceObject, PassThroughComponent incidentObject, Collision collision) {
    this.referenceObject = referenceObject;
    this.incidentObject = incidentObject;
  }
}
