package oogasalad.model.engine.api;

import java.util.Optional;
import oogasalad.model.engine.Collision;

/**
 * Contract for narrow-phase collision checks between two game objects.
 *
 * <p>This interface is intentionally scoped to pairwise evaluation. Iterating over all objects in
 * a world and deciding which pairs to evaluate is handled by higher-level engine orchestration.
 *
 * @author Georgiy Zemlevskiy
 */
public interface CollisionDetector {

  /**
   * Evaluates collisions between two objects at their current state.
   *
   * @param objectA first object in the pair
   * @param objectB second object in the pair
   * @return an {@link Optional} containing a collision record when the objects overlap, or
   *     {@link Optional#empty()} when no collision is detected
   */
  Optional<Collision> detectCollision(GameObject objectA, GameObject objectB);
}