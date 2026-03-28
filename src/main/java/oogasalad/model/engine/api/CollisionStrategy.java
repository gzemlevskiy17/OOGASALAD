package oogasalad.model.engine.api;

/** Contract to resolve overlaps and advance physics state. */
public interface CollisionStrategy {
  /** Logic to step the simulation forward based on collision resolutions. */
  void resolve(World world);
}