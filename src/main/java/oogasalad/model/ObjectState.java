package oogasalad.model;

import oogasalad.model.engine.api.Vector;

//TODO: define as interface?
public class ObjectState {

  private Vector linearPosition;
  private Vector linearVelocity;

  private double angularPosition;
  private Vector angularVelocity;

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
