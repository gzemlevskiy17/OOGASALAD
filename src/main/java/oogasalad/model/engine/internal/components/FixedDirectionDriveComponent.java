package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Vector;

public class FixedDirectionDriveComponent extends DriveComponent{
  private Vector thrustDirection;

  public FixedDirectionDriveComponent(double x, double y, double rotation, double mass,
      double friction, double maxHp, String displayName, double bodyWidth, double bodyHeight, Vector thrustDirection) {
    super(x, y, rotation, mass, friction, maxHp, displayName, bodyWidth, bodyHeight);
    this.thrustDirection = thrustDirection.normalize();

  }

  public void setThrustDirection(Vector thrustDirection) {
    this.thrustDirection = thrustDirection;
  }

  @Override
  public Vector getThrustDirection() {
    return thrustDirection;
  }
}
