package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;

public abstract class StaticComponent extends RigidBody {

  private double friction;

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


  @Override
  public double getInverseMass() {
    return 0.0;
  }

  @Override
  public double getInverseMomentOfInertia() {
    return 0.0;
  }

}
