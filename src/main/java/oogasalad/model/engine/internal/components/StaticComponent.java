package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;

public abstract class StaticComponent extends RigidBody {

  /**
   *
   * @param x
   * @param y
   * @param rotation
   * @param polygon
   * @param tag
   */
  public StaticComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag, double friction, double restitution) {
    super(x, y, rotation, polygon, tag, friction, restitution);
  }

  /**
   * Compatibility constructor for lightweight engine tests.
   */
  public StaticComponent(double x, double y, double rotation, double friction,
      double restitution) {
    this(x, y, rotation, ConvexPolygon.axisAlignedBox(x, y, 1, 1), "", friction, restitution);
  }


  //TODO: comment
  @Override
  public double getInverseMass() {
    return 0.0;
  }

  @Override
  public double getInverseMomentOfInertia() {
    return 0.0;
  }

  @Override
  public void addForce(Vector force) {
    // immovable terrain: ignore
  }

  @Override
  public boolean isAffectedByGravity() {
    return false;
  }

  @Override
  public boolean isTrigger() {
    return false;
  }

}
