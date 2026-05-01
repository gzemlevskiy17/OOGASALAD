package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.HasCollisionEffect;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.StaticComponent;

/**
 * Solves collisions between a dynamic body and a static body.
 *
 * <p><strong>Purpose:</strong> Applies impulse response, optional positional correction, and
 * optional collision effects for dynamic-vs-static collisions.
 *
 * <p>
 * <strong>Assumptions:</strong> The provided collision manifold and penetration
 * data were generated for {@code objectA} and {@code objectB} during the current simulation step.
 *
 * <p><strong>Dependencies:</strong> Delegates numerical response to {@link ImpulseSolverUtilities}
 * and gameplay side effects to {@link CollisionEffect} / {@link HasCollisionEffect} contracts.
 *
 * <p>
 * <strong>Example:</strong>
 *
 * <pre>{@code
 * DynamicStaticSolver solver = new DynamicStaticSolver();
 * solver.solve(dynamicBody, staticBody, collision, true, true);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 * @author Paulina Vvedenskaya
 */
public class DynamicStaticSolver {

  private StaticComponent referenceObject;
  private DynamicComponent incidentObject;

  private static void applyCollisionEffect(StaticComponent staticSide,
      DynamicComponent dynamicSide) {
    if (staticSide instanceof HasCollisionEffect actor) {
      CollisionEffect effect = actor.getCollisionEffect();
      if (effect != null) {
        effect.apply(dynamicSide);
      }
    }
  }

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
   * @param objectA          dynamic object in the collision
   * @param objectB          static object in the collision
   * @param collision        collision data containing manifold and penetration information
   * @param isFirstIteration true only on the first solver iteration for the collision set
   * @param solveImpulse     true when the current pass should also apply positional correction
   */
  public void solve(DynamicComponent objectA, StaticComponent objectB,
      Collision collision, boolean isFirstIteration, boolean solveImpulse) {
    if (objectA == collision.getReferenceObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    this.referenceObject = objectB;
    this.incidentObject = objectA;

    if (solveImpulse) {
      resolveImpulseAtContacts(collision, isFirstIteration);
      if (isFirstIteration) {
        applyCollisionEffect(objectB, objectA);
      }
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
   * <p>
   * <strong>Assumptions:</strong> {@code referenceObject} and {@code incidentObject} have been set
   * by {@link #solve(DynamicComponent, StaticComponent, Collision, boolean, boolean)} before
   * invocation.
   *
   * @param collision detected collision pair
   */
  private void resolveImpulseAtContacts(Collision collision, boolean isFirstIteration) {
    DynamicState referenceDynamicState = new DynamicState(); // initialize placeholder state with 0 values
    DynamicState incidentDynamicState = incidentObject.getDynamicState();

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(collision, referenceObject,
        incidentObject, incidentDynamicState, referenceDynamicState, isFirstIteration);
  }
}
