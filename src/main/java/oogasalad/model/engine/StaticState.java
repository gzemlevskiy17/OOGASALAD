package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;

/**
 * Mutable state holder for an object's linear and angular position.
 *
 * <p>Used by the engine to keep track of transform data, and depends on {@link Vector} for the
 * linear position. Callers are responsible for providing valid position values.
 * @author Georgiy Zemlevskiy
 */
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
