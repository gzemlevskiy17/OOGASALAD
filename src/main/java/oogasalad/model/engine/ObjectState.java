package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;

//TODO: define as interface?

/**
 * Mutable physics state for a single game object.
 *
 * <p>Use this class to hold linear and angular position/velocity values exchanged between the game
 * engine and objects each step.
 *
 * <p><strong>Assumptions:</strong> Angular velocity is currently stored as a vector in this model,
 * while some engine code reads only the x component as scalar angular speed.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link Vector} for linear values and stored angular
 * velocity representation.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * ObjectState state = new ObjectState(
 *     new Vector(5, 10), new Vector(1, 0), 0.0, new Vector(0, 0));
 * state.setLinearVelocity(new Vector(1, -9.8));
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class ObjectState {

  private Vector linearPosition;
  private Vector linearVelocity;

  private double angularPosition;
  private Vector angularVelocity;

  /**
   * Creates a state container.
   *
   * @param linearPosition  current world position
   * @param linearVelocity  current translational velocity
   * @param angularPosition current orientation angle
   * @param angularVelocity angular velocity representation
   */
  public ObjectState(Vector linearPosition, Vector linearVelocity, double angularPosition,
      Vector angularVelocity) {
    this.linearPosition = linearPosition;
    this.linearVelocity = linearVelocity;
    this.angularPosition = angularPosition;
    this.angularVelocity = angularVelocity;
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

  public Vector getAngularVelocity() {
    return angularVelocity;
  }

  public void setAngularVelocity(Vector angularVelocity) {
    this.angularVelocity = angularVelocity;
  }
}
