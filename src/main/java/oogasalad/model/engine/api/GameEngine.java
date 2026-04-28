package oogasalad.model.engine.api;

/**
 * Contract to step a gameplay simulation.
 */
public interface GameEngine {

  /** Advances the physics simulation for the associated World. */
  void step(double elapsedTime);
}
