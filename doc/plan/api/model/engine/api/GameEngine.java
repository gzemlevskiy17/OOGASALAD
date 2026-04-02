package oogasalad.model.engine.api;

/** Contract to step the simulation forward. */
public interface GameEngine {
  /** Advances the physics simulation for the associated World. */
  void step();
}