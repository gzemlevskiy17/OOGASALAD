package oogasalad.model.engine.api;

/** A physical connection holding two GameObjects together. */
public interface Joint {
  GameObject getObjectA();
  GameObject getObjectB();

  /**
   * Applies one constraint-solver pass for this joint.
   *
   * <p>Default no-op keeps existing test doubles source-compatible until a concrete joint
   * implementation is attached to the physics loop.
   */
  default void apply() {
  }
}
