package oogasalad.model.engine.api;

/** A physical connection holding two GameObjects together. */
public interface Joint {
  /** Returns the first endpoint attached to this joint. */
  GameObject getObjectA();

  /** Returns the second endpoint attached to this joint. */
  GameObject getObjectB();

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
