package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;

public class StaticState {

  private Vector linearPosition;
  private double angularPosition;

  public StaticState(Vector linearPosition, double angularPosition) {
    this.linearPosition = linearPosition;
    this.angularPosition = angularPosition;
  }

  public Vector getLinearPosition() {
    return linearPosition;
  }

  public void setLinearPosition(Vector linearPosition) {
    this.linearPosition = linearPosition;
  }

  public double getAngularPosition() {
    return angularPosition;
  }

  public void setAngularPosition(double angularPosition) {
    this.angularPosition = angularPosition;
  }
}
