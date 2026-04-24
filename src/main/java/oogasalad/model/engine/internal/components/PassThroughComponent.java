package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;

public abstract class PassThroughComponent extends GameComponent {

  public PassThroughComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag) {
    super(x, y, rotation, polygon, tag);
    setTag(DefaultTag.PASSTHROUGH.value());
  }

  /**
   * Compatibility constructor for lightweight engine tests.
   */
  public PassThroughComponent(double x, double y, double rotation, double friction,
      double restitution) {
    this(x, y, rotation, ConvexPolygon.axisAlignedBox(x, y, 1, 1), DefaultTag.PASSTHROUGH.value());
  }
}
