package oogasalad.model.engine.api;

import java.util.Optional;
import oogasalad.model.engine.Collision;

/**
 * Contract for resolving a single detected collision between two objects.
 *
 * <p>Use this interface to encapsulate collision response strategy (for example, impulse-based
 * response) separately from broad-phase pair generation and narrow-phase detection.
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
 * CollisionSolver solver = new ImpulseCollisionSolver();
 * solver.solve(collision);
 * }</pre>
 *
 * <p><strong>Details:</strong> This interface is intentionally pair-scoped; orchestration code is
 * responsible for deciding which collisions to pass into {@link #solve(Collision)} and in what
 * order.
 *
 * @author Georgiy Zemlevskiy
 */
public interface CollisionSolver {

  /**
   * Applies a collision response strategy to one collision record.
   *
   * <p>Use this method to mutate involved object states (for example, velocity and/or position)
   * based on collision geometry and material properties.
   *
   * <p><strong>Assumptions:</strong> {@code collision} is non-null and references valid objects.
   * Implementations may no-op when data is insufficient (for example, missing contact points).
   *
   * @param collision collision data for one object pair at the current simulation step
   */
  void solve(Optional<Collision> collision);
  //TODO: is it best that this mutates, or returns new object state?
}
