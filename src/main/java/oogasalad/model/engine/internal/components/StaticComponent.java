package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;

public abstract class StaticComponent extends GameComponent {

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
  public StaticComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag, double friction, double restitution) {
    super(x, y, rotation, polygon, tag);
    this.friction = friction;
    this.restitution = restitution;
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

  public double getRestitution() {
    return restitution;
  }

  public void setRestitution(double restitution) {
    this.restitution = restitution;
  }
}
