package oogasalad.model.engine;

import oogasalad.model.engine.internal.components.StaticComponent;

/**
 * The solver for the Dynamic - PassThrough Collision
 *
 * @author Iurii Beliaev
 */
public class StaticStaticSolver {

  private StaticComponent referenceObject;
  private StaticComponent incidentObject;

  /**
   * solve() for static-static solver For now, do nothing
   *
   * @param referenceObject - static object 1 in the collision
   * @param incidentObject  - static object 2 in the collision
   * @param collision       - Collision type object representing the collision between the two
   */
  public void solve(StaticComponent referenceObject, StaticComponent incidentObject,
      Collision collision) {
    this.referenceObject = referenceObject;
    this.incidentObject = incidentObject;
  }
}
