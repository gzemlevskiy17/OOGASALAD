package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;

//TODO: define as interface?

/**
 * Mutable physics state for a single game object.
 *
 * <p>Use this class to hold linear and angular position/velocity values exchanged between the game
 * engine and objects each step.
 *
 * <p><strong>Assumptions:</strong>
 * Angular motion is represented with scalar values in this class, so callers must pass and read
 * angular values consistently.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link Vector} for linear motion values and on the
 * engine integration helpers in this package.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * ObjectState state = new ObjectState(
 *     new Vector(5, 10), new Vector(1, 0), new Vector(0, -9.8), 0.0, 0.0, 0.0);
 * state.integrateObjectState(1.0 / 60.0);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class ObjectState {

  private Vector linearPosition;
  private Vector linearVelocity;
  private Vector linearAcceleration;

  private double angularPosition;
  private double angularVelocity;
  private double angularAcceleration;

  /**
   * Creates a state container.
   *
   * @param linearPosition  current world position
   * @param linearVelocity      current linear velocity
   * @param linearAcceleration  current linear acceleration
   * @param angularPosition     current orientation angle
   * @param angularVelocity angular velocity representation
   */
  public ObjectState(Vector linearPosition, Vector linearVelocity, Vector linearAcceleration,
      double angularPosition,
      double angularVelocity, double angularAcceleration) {
    this.linearPosition = linearPosition;
    this.linearVelocity = linearVelocity;
    this.linearAcceleration = linearAcceleration;

    this.angularPosition = angularPosition;
    this.angularVelocity = angularVelocity;
    this.angularAcceleration = angularAcceleration;
  }

  /**
   * Creates a state with the given position and zero linear and angular motion. Reserved for use
   * with Static Objects.
   *
   * @param linearPosition  current world-space position
   * @param angularPosition current orientation angle
   */
  public ObjectState(Vector linearPosition, double angularPosition) {
    this(linearPosition, new Vector(0, 0), new Vector(0, 0), angularPosition, 0, 0);
  }

  public Vector getLinearPosition() {
    return linearPosition;
  }

  public void setLinearPosition(Vector linearPosition) {
    this.linearPosition = linearPosition;
  }

  public Vector getLinearVelocity() {
    return linearVelocity;
  }

  public void setLinearVelocity(Vector linearVelocity) {
    this.linearVelocity = linearVelocity;
  }

  public double getAngularPosition() {
    return angularPosition;
  }

  public void setAngularPosition(double angularPosition) {
    this.angularPosition = angularPosition;
  }

  public double getAngularVelocity() {
    return angularVelocity;
  }

  public void setAngularVelocity(double angularVelocity) {
    this.angularVelocity = angularVelocity;
  }

  public Vector getLinearAcceleration() {
    return linearAcceleration;
  }

  public void setLinearAcceleration(Vector linearAcceleration) {
    this.linearAcceleration = linearAcceleration;
  }

  public double getAngularAcceleration() {
    return angularAcceleration;
  }

  public void setAngularAcceleration(double angularAcceleration) {
    this.angularAcceleration = angularAcceleration;
  }

  /**
   * Advances the state forward by the given time step using simple Euler integration.
   *
   * <p>This updates velocity before position for both linear and angular motion.
   *
   * @param elapsedTime simulation time step in seconds
   */
  public void integrateObjectState(double elapsedTime) {
    linearVelocity = integrateVector(linearVelocity, linearAcceleration, elapsedTime);
    linearPosition = integrateVector(linearPosition, linearVelocity, elapsedTime);

    angularVelocity = integrateScalar(angularVelocity, angularAcceleration, elapsedTime);
    angularPosition = integrateScalar(angularPosition, angularVelocity, elapsedTime);
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
}
