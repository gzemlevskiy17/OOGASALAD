package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.ObjectState;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

public abstract class GameComponent implements GameObject {

  private ObjectState state;
  private double mass;
  private double momentOfInertia;
  private double restitution;
  private double friction;
  private ConvexPolygon polygon;
  private Vector force;
  private boolean active; //TODO: depends on how people up the stack implement it
  private boolean affectedByGravity;
  private boolean trigger; //TODO: not needed?

  /**
   *
   * @param x
   * @param y
   * @param rotation
   * @param mass
   * @param friction
   */
  public GameComponent(double x, double y, double rotation, double mass, double friction) {
    this.state = new ObjectState(new Vector(x, y), new Vector(0, 0), new Vector(0, 0), rotation, 0,
        0
    );
    setMass(mass);
    setFriction(friction);
    this.force = new Vector(0, 0);
    this.active = true;
    this.affectedByGravity = true;
    this.trigger = false;
  }

  public void setPosition(double x, double y, double rotation) {
    state.setLinearPosition(new Vector(x, y));
    state.setAngularPosition(rotation);
  }

  @Override
  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    if (mass < 0) {
      throw new IllegalArgumentException(EngineError.MASS_MUST_BE_NON_NEGATIVE.message());
    }
    this.mass = mass;
  }

  @Override
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
  public Vector getForce() {
    return force;
  }

  @Override
  public void setForce(Vector force) {
    this.force = force;
  }

  @Override
  public void addForce(Vector force) {
    this.force = this.force.add(force);
  }

  @Override
  public ConvexPolygon getPolygon() {
    return polygon;
  }

  protected void setPolygon(ConvexPolygon polygon) {
    this.polygon = polygon;
  }

  @Override
  public ObjectState getState() {
    return state;
  }

  @Override
  public void setState(ObjectState state) {
    this.state = state;
  }

  @Override
  public double getMomentOfInertia() {
    return momentOfInertia;
  }

  @Override
  public void setMomentOfInertia(double momentOfInertia) {
    this.momentOfInertia = momentOfInertia;
  }

  @Override
  public double getRestitution() {
    return restitution;
  }

  @Override
  public void setRestitution(double restitution) {
    this.restitution = restitution;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean isAffectedByGravity() {
    return affectedByGravity;
  }

  public void setAffectedByGravity(boolean affectedByGravity) {
    this.affectedByGravity = affectedByGravity;
  }

  @Override
  public boolean isTrigger() {
    return trigger;
  }

  protected void setTrigger(boolean trigger) {
    this.trigger = trigger;
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  public void destroy() {
    deactivate();
  }

  @Override
  public double getX() {
    return state.getLinearPosition().x();
  }

  @Override
  public double getY() {
    return state.getLinearPosition().y();
  }

  @Override
  public double getRotation() {
    return state.getAngularPosition();
  }

  protected void resetForce() {
    this.force = new Vector(0, 0);
  }
}
