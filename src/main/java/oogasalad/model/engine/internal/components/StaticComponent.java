package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;

public abstract class StaticComponent extends GameComponent {

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
      String tag, double friction) {
    super(x, y, rotation, polygon, tag);
    this.friction = friction;
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
  public void setRestitution(double restitution) {
    // optional: store a field if your solver uses restitution on statics; else no-op
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
