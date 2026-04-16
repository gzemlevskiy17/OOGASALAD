package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.DefaultTag;

/**
 * Concrete dynamic component tagged as pigs.
 */
public class PigComponent extends DynamicComponent {

  public PigComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTag(DefaultTag.PIGS.value());
  }
}

