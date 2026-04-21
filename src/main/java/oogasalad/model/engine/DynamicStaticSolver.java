package oogasalad.model.engine;

import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.StaticComponent;

public class DynamicStaticSolver {
  //TODO: comment

  private StaticComponent referenceObject;
  private DynamicComponent incidentObject;

  /**
   * Resolves one collision using the impulse solver pipeline.
   *
   * <p>Use this method from engine collision orchestration after detection has produced per-pair
   * collision data for the current step.
   *
   * <p><strong>Assumptions:</strong> {@code collision} is non-null and contains enough contact
   * and normal information for response calculations.
   *
   * @param objectA   reference object whose collision normal is used for response
   * @param objectB   incident object being resolved against the reference object
   * @param collision collision data containing manifold and penetration information
   */
  public void solve(DynamicComponent objectA, StaticComponent objectB,
      Collision collision) {
    //TODO: move this into the dispatcher. document the convention
    if (objectA == collision.getReferenceObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    this.referenceObject = objectB;
    this.incidentObject = objectA;
    //TODO: a and b are no longer technically incident and reference

    resolveImpulseAtContacts(collision);
    ImpulseSolverUtilities.applyPositionalCorrection(collision, referenceObject, incidentObject);
  }

  /**
   * Applies impulse-based collision response at each manifold contact.
   *
   * <p>This updates linear and angular velocities in-place on both colliding object states.
   *
   * @param collision detected collision pair
   */
  private void resolveImpulseAtContacts(Collision collision) {
    //TODO: refactor
    StaticState referenceStaticState = referenceObject.getStaticState();
    StaticState incidentStaticState = incidentObject.getStaticState();
    DynamicState referenceDynamicState = new DynamicState(); //initialize placeholder state with 0 values
    DynamicState incidentDynamicState = incidentObject.getDynamicState();

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(collision, referenceObject,
        incidentObject, incidentStaticState, referenceStaticState, incidentDynamicState,
        referenceDynamicState);
  }
}
