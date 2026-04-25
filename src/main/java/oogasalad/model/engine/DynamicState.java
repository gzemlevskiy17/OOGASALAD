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
 * DynamicState state = new DynamicState(
 *     new Vector(5, 10), new Vector(1, 0), new Vector(0, -9.8), 0.0, 0.0, 0.0);
 * state.integrateObjectState(1.0 / 60.0);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class DynamicState {

  private Vector linearVelocity;
  private Vector linearAcceleration;

  private double angularVelocity;
  private double angularAcceleration;

  /**
   * Creates a state container.
   *
   * @param linearVelocity     current linear velocity
   * @param linearAcceleration current linear acceleration
   * @param angularVelocity    angular velocity representation
   */
  public DynamicState(Vector linearVelocity, Vector linearAcceleration,
      double angularVelocity, double angularAcceleration) {
    this.linearVelocity = linearVelocity;
    this.linearAcceleration = linearAcceleration;

    this.angularVelocity = angularVelocity;
    this.angularAcceleration = angularAcceleration;
  }

  public DynamicState() {
    this.linearVelocity = new Vector(0, 0);
    this.linearAcceleration = new Vector(0, 0);

    this.angularVelocity = 0;
    this.angularAcceleration = 0;
  }

  public Vector getLinearVelocity() {
    return linearVelocity;
  }

  public void setLinearVelocity(Vector linearVelocity) {
    this.linearVelocity = linearVelocity;
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
}
