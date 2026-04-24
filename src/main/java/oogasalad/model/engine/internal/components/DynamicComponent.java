package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.DynamicState;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.Vector;

public abstract class DynamicComponent extends RigidBody {

  private final DynamicState dynamicState;
  private double mass;
  private double momentOfInertia;
  private Vector force;
  private boolean affectedByGravity;

  /**
   *
   * @param x
   * @param y
   * @param rotation
   * @param polygon
   * @param tag
   */
  public DynamicComponent(double x, double y, double rotation,
      ConvexPolygon polygon, String tag, double friction, double mass, double momentOfInertia,
      double restitution, boolean affectedByGravity) {
    super(x, y, rotation, polygon, tag, friction, restitution);
    this.dynamicState = new DynamicState();
    this.mass = mass;
    this.momentOfInertia = momentOfInertia;
    this.affectedByGravity = affectedByGravity;
    this.force = new Vector(0, 0);
  }

  /**
   * Exposes mutable linear/angular state for integration and tests.
   */
  public DynamicState getDynamicState() {
    return dynamicState;
  }

  public Vector getForce() {
    return force;
  }

  public void setForce(Vector force) {
    this.force = force;
  }

  public void addForce(Vector force) {
    this.force = this.force.add(force);
  }

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
  public double getInverseMass() {
    return 1 / mass;
  }

  @Override
  public double getInverseMomentOfInertia() {
    return 1 / momentOfInertia;
  }

  public boolean isAffectedByGravity() {
    return affectedByGravity;
  }

  public void setAffectedByGravity(boolean affectedByGravity) {
    this.affectedByGravity = affectedByGravity;
  }

  @Override
  public boolean isTrigger() {
    return false;
  }

  public double getMomentOfInertia() {
    return momentOfInertia;
  }

  public void setMomentOfInertia(double momentOfInertia) {
    this.momentOfInertia = momentOfInertia;
  }

  public void resetForce() {
    this.force = new Vector(0, 0);
  }

  /**
   * Advances the state forward by the given time step using simple Euler integration.
   *
   * <p>This updates velocity before position for both linear and angular motion.
   *
   * @param elapsedTime simulation time step in seconds
   */
  public void integrateObjectState(double elapsedTime) {
    StaticState staticState = getStaticState();

    dynamicState.setLinearVelocity(
        integrateVector(dynamicState.getLinearVelocity(), dynamicState.getLinearAcceleration(),
            elapsedTime));
    staticState.setLinearPosition(
        integrateVector(staticState.getLinearPosition(), dynamicState.getLinearVelocity(),
            elapsedTime));

    dynamicState.setAngularVelocity(integrateScalar(dynamicState.getAngularVelocity(),
        dynamicState.getAngularAcceleration(), elapsedTime));
    staticState.setAngularPosition(integrateScalar(staticState.getAngularPosition(),
        dynamicState.getAngularVelocity(), elapsedTime));
  }

  /**
   * Integrates a 2D vector component-wise over time.
   *
   * @param previousVector previous value being advanced
   * @param integrand      value added per unit time
   * @param elapsedTime    time step in seconds
   * @return integrated vector
   */
  private Vector integrateVector(Vector previousVector, Vector integrand, double elapsedTime) {
    double newX = integrateScalar(previousVector.x(), integrand.x(), elapsedTime);
    double newY = integrateScalar(previousVector.y(), integrand.y(), elapsedTime);

    return new Vector(newX, newY);
  }

  /**
   * Integrates a scalar value over time.
   *
   * @param previousValue  previous value being advanced
   * @param integrandValue value added per unit time
   * @param elapsedTime    time step in seconds
   * @return integrated scalar
   */
  private double integrateScalar(double previousValue, double integrandValue, double elapsedTime) {
    return previousValue + integrandValue * elapsedTime;
  }

  public void applyForceToAcceleration() {
    double m = getMass();
    if (m <= 0) {
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      return;
    }

    Vector net = getForce();

    if (net == null) {
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      return;
    }

    dynamicState.setLinearAcceleration(net.multiply(1.0 / m));
  }
}
