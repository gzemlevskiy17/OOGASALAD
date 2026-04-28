package oogasalad.model.engine.api;

import oogasalad.model.engine.Collision;

/**
 * Contract for resolving a single detected collision between two objects.
 *
 * <p>Use this interface to encapsulate collision response strategy separately from broad-phase
 * pair generation and narrow-phase detection.
 *
 * <p><strong>Assumptions:</strong> Input collisions contain enough manifold/penetration data for
 * the selected solver strategy. If collision metadata is incomplete, implementations may skip
 * resolution or produce no state changes.
 *
 * <p><strong>Dependencies:</strong> Depends on the engine {@link Collision} model and object/state
 * APIs referenced by concrete solver implementations.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionSolver solver = new DynamicDynamicSolver();
 * solver.solve(objectA, objectB, collision, true, true);
 * }</pre>
 *
 * <p><strong>Details:</strong> This interface is intentionally pair-scoped; orchestration code is
 * responsible for deciding which collisions to pass into
 * {@link #solve(GameObject, GameObject, Collision, boolean, boolean)} and whether the current pass
 * should apply impulses or only position correction.
 *
 * @author Georgiy Zemlevskiy
 */
@FunctionalInterface
public interface CollisionSolver {

  /**
   * Applies a collision response strategy to one collision record.
   *
   * <p>Use this method to mutate involved object states based on collision geometry and material
   * properties. {@code isFirstIteration} marks the first solver pass for one frame's collision, and
   * {@code solveImpulse} indicates whether the implementation should also apply positional
   * correction during the current pass.
   *
   * <p><strong>Assumptions:</strong> {@code collision} is non-null and references valid objects.
   * Implementations may no-op when data is insufficient (for example, missing contact points) or
   * when the current pass is position-only.
   *
   * @param objectA          first object in solver order
   * @param objectB          second object in solver order
   * @param collision        collision data for one object pair at the current simulation step
   * @param isFirstIteration true only on the first solver iteration for the collision set
   * @param solveImpulse     true when the current pass should apply positional correction too
   */
  void solve(GameObject objectA, GameObject objectB, Collision collision, boolean isFirstIteration,
      boolean solveImpulse);
}
