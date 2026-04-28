package oogasalad.model.engine.api;

/**
 * Contract to step and adjust a gameplay simulation.
 */
public interface GameEngine {

  /** Advances the physics simulation for the associated World. */
  void step(double elapsedTime);

  /**
   * Updates the gravity vector used by future simulation steps.
   */
  void setGravity(Vector gravityVector);
}
