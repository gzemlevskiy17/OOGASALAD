package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.DynamicState;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.Vector;

public abstract class DynamicComponent extends RigidBody {

  private static final double LINEAR_SLEEP_THRESHOLD = 1.0;
  private static final double ANGULAR_SLEEP_THRESHOLD = 0.01;
  private static final double TIME_TO_SLEEP = 0.5;

  private final DynamicState dynamicState;
  private double mass;
  private double momentOfInertia;
  private Vector force;
  private boolean affectedByGravity;
  private boolean sleepingAllowed;
  private boolean sleeping;
  private double sleepTime;

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
    setMass(mass);
    setMomentOfInertia(momentOfInertia);
    this.affectedByGravity = affectedByGravity;
    this.force = new Vector(0, 0);
    this.sleepingAllowed = true;
    this.sleeping = false;
    this.sleepTime = 0.0;
  }

  /**
   * Compatibility constructor for lightweight engine tests.
   */
  public DynamicComponent(double x, double y, double rotation, double mass, double restitution) {
    this(x, y, rotation, ConvexPolygon.axisAlignedBox(x, y, 1, 1), "", 0.0, mass, 1.0,
        restitution, false);
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
    if (this.sleeping && force != null && force.magnitude() > 0.0) {
      setSleeping(false);
    }
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
    if (mass <= 0.0) {
      return 0.0;
    }
    return 1 / mass;
  }

  @Override
  public double getInverseMomentOfInertia() {
    if (momentOfInertia <= 0.0) {
      return 0.0;
    }
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

  public boolean isSleepingAllowed() {
    return sleepingAllowed;
  }

  public void setSleepingAllowed(boolean sleepingAllowed) {
    this.sleepingAllowed = sleepingAllowed;
    if (!sleepingAllowed) {
      setSleeping(false);
    }
  }

  public boolean isSleeping() {
    return sleeping;
  }

  public void setSleeping(boolean sleeping) {
    this.sleeping = sleeping;
    if (sleeping) {
      dynamicState.setLinearVelocity(new Vector(0, 0));
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      dynamicState.setAngularVelocity(0.0);
      dynamicState.setAngularAcceleration(0.0);
      resetForce();
    } else {
      sleepTime = 0.0;
    }
  }

  public void updateSleepState(double elapsedTime) {
    if (!sleepingAllowed) {
      return;
    }
    double linearSpeed = dynamicState.getLinearVelocity().magnitude();
    double angularSpeed = Math.abs(dynamicState.getAngularVelocity());
    if (linearSpeed > LINEAR_SLEEP_THRESHOLD || angularSpeed > ANGULAR_SLEEP_THRESHOLD) {
      sleepTime = 0.0;
      sleeping = false;
      return;
    }
    sleepTime += elapsedTime;
    if (sleepTime >= TIME_TO_SLEEP) {
      setSleeping(true);
    }
  }

  public double getMomentOfInertia() {
    return momentOfInertia;
  }

  public void setMomentOfInertia(double momentOfInertia) {
    if (momentOfInertia < 0) {
      throw new IllegalArgumentException(
          EngineError.MOMENT_OF_INERTIA_MUST_BE_NON_NEGATIVE.message());
    }
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
    if (sleeping) {
      return;
    }
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
    if (sleeping) {
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      return;
    }
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
