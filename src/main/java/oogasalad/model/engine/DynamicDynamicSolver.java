package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Impulse-based collision response implementation for a single dynamic-dynamic collision pair.
 *
 * <p><strong>Purpose:</strong> Resolves detected collisions by computing contact impulses. When
 * the current pass requests it, the solver also applies positional correction to separate
 * overlapping bodies.
 *
 * <p>
 * <strong>Assumptions:</strong> Incoming {@link Collision} objects reference
 * valid {@link DynamicComponent} objects and states. Polygon winding is consistent with helper
 * expectations, and mass and moment-of-inertia values are non-negative. Degenerate geometry or
 * missing manifold data can cause the solver to skip useful work or produce incomplete
 * corrections.
 *
 * <p><strong>Dependencies:</strong> Relies on {@link Collision}, {@link DynamicState},
 * {@link GameObject}, {@link Vector}, and the {@link CollisionSolver} interface contract. Uses
 * internal {@link ContactPointPhysics} class to track per-contact physics data.
 *
 * <p>
 * <strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionSolver solver = new DynamicDynamicSolver();
 * solver.solve(dynamicObjectA, dynamicObjectB, collision, true, true);
 * }</pre>
 *
 * <p><strong>Details:</strong> Code and formulas borrowed from
 * <a href=
 * "https://timallanwheeler.com/blog/2024/08/01/2d-collision-detection-and-resolution/,https://en.wikipedia.org/wiki/Collision_response#Impulse-based_reaction_model">...</a>.
 * The solver computes impulse response first and only applies positional correction when the caller
 * requests it.
 *
 * @author Georgiy Zemlevskiy
 *
 */
public class DynamicDynamicSolver {

  private DynamicComponent referenceObject;
  private DynamicComponent incidentObject;

  /**
   * Resolves one collision using the impulse solver pipeline.
   *
   * <p>
   * Use this method from engine collision orchestration after detection has produced per-pair
   * collision data for the current step.
   *
   * <p>
   * <strong>Assumptions:</strong> {@code collision} is non-null and contains
   * enough contact and normal information for response calculations.
   *
   * @param objectA          reference object whose collision normal is used for response
   * @param objectB          incident object being resolved against the reference object
   * @param collision        collision data containing manifold and penetration information
   * @param isFirstIteration true only on the first solver iteration for the collision set
   * @param solveImpulse     true when the current pass should also apply positional correction
   */
  public void solve(DynamicComponent objectA, DynamicComponent objectB,
      Collision collision, boolean isFirstIteration, boolean solveImpulse) {
    // TODO: move this into the dispatcher. document the convention
    if (objectA == collision.getIncidentObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    this.referenceObject = objectA;
    this.incidentObject = objectB;
    // TODO: a and b technically are no longer incident and reference

    if (solveImpulse) {
      resolveImpulseAtContacts(collision, isFirstIteration);
    } else {
      ImpulseSolverUtilities.applyPositionalCorrection(collision, referenceObject, incidentObject);
    }
  }

  /**
   * Applies impulse-based collision response at each manifold contact.
   *
   * <p>
   * This updates linear and angular velocities in-place on both colliding object states.
   *
   * @param collision detected collision pair
   */
  private void resolveImpulseAtContacts(Collision collision, boolean isFirstIteration) {
    DynamicState referenceDynamicState = referenceObject.getDynamicState();
    DynamicState incidentDynamicState = incidentObject.getDynamicState();

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(collision, referenceObject,
        incidentObject, incidentDynamicState, referenceDynamicState, isFirstIteration);
  }
}
