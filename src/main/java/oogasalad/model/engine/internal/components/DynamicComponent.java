package oogasalad.model.engine.internal.components;

import java.util.LinkedHashMap;
import java.util.Map;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.DynamicState;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.Vector;

/**
 * Base rigid-body component for movable simulation objects.
 *
 * <p><strong>Purpose:</strong> Stores dynamic state (velocity/acceleration), force accumulation,
 * mass properties, and sleep behavior used by the physics update loop.
 *
 * <p><strong>Assumptions:</strong> Mass and moment of inertia are non-negative. Callers clear
 * accumulated forces each step after integration.
 *
 * <p><strong>Dependencies:</strong> Extends {@link RigidBody} and updates {@link StaticState}
 * and {@link DynamicState} each frame.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * dynamicComponent.addForce(new Vector(0, 980));
 * dynamicComponent.applyForceToAcceleration();
 * dynamicComponent.integrateObjectState(dt);
 * dynamicComponent.resetForces();
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 * @author Paulina Vvedenskaya
 */
public abstract class DynamicComponent extends RigidBody {

  private static final double LINEAR_SLEEP_THRESHOLD = 1.0;
  private static final double ANGULAR_SLEEP_THRESHOLD = 0.01;
  private static final double TIME_TO_SLEEP = 0.5;

  private final DynamicState dynamicState;
  private double mass;
  private double momentOfInertia;
  private Vector force;
  private double torque;
  private boolean affectedByGravity;
  private boolean sleepingAllowed;
  private boolean sleeping;
  private double sleepTime;
  private final Map<String, Vector> externalForces = new LinkedHashMap<>();

  /**
   * Creates a dynamic component with full geometric and physical configuration.
   *
   * @param x                 initial x position
   * @param y                 initial y position
   * @param rotation          initial angular position in radians
   * @param polygon           collision polygon in world coordinates
   * @param tag               lookup tag for gameplay queries
   * @param friction          surface friction coefficient (non-negative)
   * @param mass              body mass (non-negative, zero treated as immovable)
   * @param momentOfInertia   angular inertia (non-negative)
   * @param restitution       bounciness in [0, 1]
   * @param affectedByGravity whether gravity should be applied in force accumulation
   * @throws IllegalArgumentException when mass, inertia, friction, or restitution is invalid
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
   * Adds torque to the current accumulator for the next acceleration update.
   *
   * @param torque torque delta to accumulate
   */
  public void addTorque(double torque) {
    if (this.sleeping && Math.abs(torque) > 0.0) {
      setSleeping(false);
    }
    this.torque += torque;
  }

  /** Clears accumulated torque for the current simulation step. */
  public void resetTorque() {
    this.torque = 0.0;
  }

  public double getTorque() {
    return torque;
  }

  public void setTorque(double torque) {
    this.torque = torque;
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

  /**
   * Adds force to the current accumulator for the next acceleration update.
   *
   * @param force force delta to accumulate
   */
  public void addForce(Vector force) {
    if (this.sleeping && force != null && force.magnitude() > 0.0) {
      setSleeping(false);
    }
    this.force = this.force.add(force);
  }

  public double getMass() {
    return mass;
  }

  /**
   * Sets mass used by translational dynamics.
   *
   * @param mass body mass (must be positive)
   * @throws IllegalArgumentException when {@code mass < 0}
   */
  public void setMass(double mass) {
    if (mass <= 0) {
      throw new IllegalArgumentException(EngineError.MASS_MUST_BE_NON_NEGATIVE.message());
    }
    this.mass = mass;
  }

  /**
   * Returns inverse mass used by impulse calculations.
   *
   * @return {@code 1 / mass} for positive mass, otherwise 0.0
   */
  @Override
  public double getInverseMass() {
    if (mass <= 0.0) { //TODO: check needed?
      return 0.0;
    }
    return 1 / mass;
  }

  /**
   * Returns inverse angular inertia used by rotational impulse calculations.
   *
   * @return {@code 1 / momentOfInertia} for positive inertia, otherwise 0.0
   */
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

  /**
   * Sets sleep state and clears dynamic motion when entering sleep.
   *
   * @param sleeping true to put the body to sleep
   */
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

  /**
   * Updates sleep timer based on current linear and angular speed thresholds.
   *
   * @param elapsedTime simulation step in seconds
   */
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

  /**
   * Sets angular inertia used by rotational dynamics.
   *
   * @param momentOfInertia inertia value (must be positive)
   * @throws IllegalArgumentException when {@code momentOfInertia < 0}
   */
  public void setMomentOfInertia(double momentOfInertia) {
    if (momentOfInertia <= 0) {
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

  /**
   * Computes linear and angular acceleration from accumulated force and torque.
   *
   * <p>Sleeping bodies and zero-mass bodies receive zero acceleration.
   */
  public void applyForceToAcceleration() {
    double m = getMass();
    if (sleeping || m <= 0) {
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      dynamicState.setAngularAcceleration(0.0);
      return;
    }

    Vector netForce = getForce();
    double netTorque = getTorque();

    if (netForce == null) {
      dynamicState.setLinearAcceleration(new Vector(0, 0));
      return;
    } //TODO: need this check?

    dynamicState.setLinearAcceleration(netForce.multiply(1.0 / m));
    dynamicState.setAngularAcceleration(netTorque * getInverseMomentOfInertia());
  }

  /**
   * Adds or replaces a named external force applied each frame until removed.
   */
  public void addExternalForce(String id, Vector forceVector) {
    if (forceVector != null && forceVector.magnitude() > 0 && this.sleeping) {
      setSleeping(false);
    }
    externalForces.put(id, forceVector);
  }

  /**
   * Removes a named external force.
   */
  public void removeExternalForce(String id) {
    externalForces.remove(id);
  }

  /**
   * Removes all named external forces.
   */
  public void clearExternalForces() {
    externalForces.clear();
  }

  /**
   * Returns the sum of all active named external forces.
   */
  public Vector getExternalForceTotal() {
    Vector total = new Vector(0, 0);
    for (Vector f : externalForces.values()) {
      if (f != null) {
        total = total.add(f);
      }
    }
    return total;
  }

  /** Clears both torque and linear force accumulators. */
  public void resetForces() {
    resetTorque();
    resetForce();
  }
}
