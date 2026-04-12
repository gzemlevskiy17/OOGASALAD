package oogasalad.model.engine.api;

import oogasalad.model.engine.internal.components.GameComponent;

/** A physical connection holding two GameObjects together. */
public interface Joint {

  /**
   * Returns the first endpoint attached to this joint.
   */
  GameComponent getComponentA();

  /**
   * Returns the second endpoint attached to this joint.
   */
  GameComponent getComponentB();

  /**
   * Applies one constraint-solver pass for this joint using the given timestep.
   *
   * @param dt timestep used by joints that depend on elapsed time
   */
  default void apply(double dt) {
  }

  /**
   * Backward-compatible no-arg joint solve step.
   */
  default void apply() {
    apply(1.0);
  }
}
