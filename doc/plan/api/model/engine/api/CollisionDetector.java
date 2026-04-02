package oogasalad.model.engine.api;

/** Contract for finding overlapping hitboxes within a World. */
public interface CollisionDetector {
  /** Analyzes the world state to identify current overlaps. */
  void detectCollisions(World world);
}