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

  /**
   * Enforces the joint's velocity-level constraint, if any. Default is a no-op so soft joints
   * (springs) don't need to override. Rigid/revolute joints override to zero the relative velocity
   * at the shared anchor, which lets a force or collision impulse on one body propagate to
   * rigidly-connected bodies in the same step.
   */
  default void applyVelocity() {
  }
}
