package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;

public abstract class RigidBody extends GameComponent {
  //TODO: comment

  private double friction;
  private double restitution;

  /**
   *
   * @param x
   * @param y
   * @param rotation
   * @param polygon
   * @param tag
   */
  public RigidBody(double x, double y, double rotation, ConvexPolygon polygon,
      String tag, double friction, double restitution) {
    super(x, y, rotation, polygon, tag);
    setFriction(friction);
    setRestitution(restitution);
  }


  public double getFriction() {
    return friction;
  }

  public void setFriction(double friction) {
    if (friction < 0) {
      throw new IllegalArgumentException(EngineError.FRICTION_MUST_BE_NON_NEGATIVE.message());
    }
    this.friction = friction;
  }

  public double getRestitution() {
    return restitution;
  }

  public void setRestitution(double restitution) {
    if (restitution < 0 || restitution > 1) {
      throw new IllegalArgumentException(EngineError.RESTITUTION_OUT_OF_RANGE.message());
    }
    this.restitution = restitution;
  }

  public abstract double getInverseMass();

  public abstract double getInverseMomentOfInertia();
}
