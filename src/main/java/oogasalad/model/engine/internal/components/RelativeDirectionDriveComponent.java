package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Vector;

public class RelativeDirectionDriveComponent extends DriveComponent {

  private Vector relativeThrustDirection;

  public RelativeDirectionDriveComponent(double x, double y, double rotation, double mass,
      double friction, double maxHp, String displayName, double bodyWidth, double bodyHeight,
      Vector relativeThrustDirection) {
    super(x, y, rotation, mass, friction, maxHp, displayName, bodyWidth, bodyHeight);
    this.relativeThrustDirection = relativeThrustDirection.normalize();

  }

  public void setRelativeThrustDirection(Vector relativeThrustDirection) {
    this.relativeThrustDirection = relativeThrustDirection;
  }

  @Override
  public Vector getThrustDirection() {
    return relativeThrustDirection.rotate(getStaticState().getAngularPosition());
  }
}
