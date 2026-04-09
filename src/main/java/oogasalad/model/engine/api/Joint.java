package oogasalad.model.engine.api;

/** A physical connection holding two GameObjects together. */
public interface Joint {
  GameObject getObjectA();
  GameObject getObjectB();

  /**
   * Applies one constraint-solver pass for this joint using the given timestep.
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
