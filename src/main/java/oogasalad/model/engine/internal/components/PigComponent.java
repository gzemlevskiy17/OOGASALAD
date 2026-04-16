package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;

/**
 * Concrete dynamic component tagged as pigs.
 */
public class PigComponent extends DynamicComponent {

  private static final double DEFAULT_SIZE = 40.0;

  public PigComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTag(DefaultTag.PIGS.value());
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(getX(), getY(), DEFAULT_SIZE, DEFAULT_SIZE));
  }

}

