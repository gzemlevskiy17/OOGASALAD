package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Vector;

/**
 * Base for non-physical world objects that detect overlap but do not produce a collision response.
 * Provides sensible defaults for the physical {@link oogasalad.model.engine.api.GameObject}
 * contract: zero friction, no gravity, no force accumulation, no restitution, and
 * {@code isTrigger() == true}. Subclasses may override any of these if their semantics differ.
 *
 * @author Georgiy Zemlevskiy
 * @author Paulina Vvedenskaya
 */
public abstract class PassThroughComponent extends GameComponent {

  public PassThroughComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag) {
    super(x, y, rotation, polygon, tag);
    setTag(DefaultTag.PASSTHROUGH.value());
  }

  @Override
  public void addForce(Vector force) {
    // pass-through bodies do not accumulate force
  }

  @Override
  public void setRestitution(double restitution) {
    // pass-through bodies have no rigid-body restitution
  }

  @Override
  public double getFriction() {
    return 0.0;
  }

  @Override
  public boolean isAffectedByGravity() {
    return false;
  }

  @Override
  public boolean isTrigger() {
    return true;
  }

  /**
   * Compatibility constructor for lightweight engine tests.
   */
  public PassThroughComponent(double x, double y, double rotation, double friction,
      double restitution) {
    this(x, y, rotation, ConvexPolygon.axisAlignedBox(x, y, 1, 1), DefaultTag.PASSTHROUGH.value());
  }
}
