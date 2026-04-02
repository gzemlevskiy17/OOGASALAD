package oogasalad.model.engine.api;

/** A physical connection holding two GameObjects together. */
public interface Joint {
  GameObject getObjectA();
  GameObject getObjectB();
}