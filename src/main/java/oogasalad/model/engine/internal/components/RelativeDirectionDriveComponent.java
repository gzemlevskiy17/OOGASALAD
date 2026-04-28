package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Vector;

/**
 * Drive component whose thrust direction rotates with the component's body.
 *
 * <p>This matches engines and fans that should keep pushing relative to the vehicle frame. The
 * stored direction is normalized once and then rotated by the component's current angle whenever
 * thrust is queried.
 *
 * @author Paulina Vvedenskaya
 * @author Georgiy Zemlevskiy
 */
public class RelativeDirectionDriveComponent extends DriveComponent {

  private Vector relativeThrustDirection;

  public RelativeDirectionDriveComponent(double x, double y, double rotation, double mass,
      double friction, double maxHp, String displayName, double bodyWidth, double bodyHeight,
      Vector relativeThrustDirection) {
    super(x, y, rotation, mass, friction, maxHp, displayName, bodyWidth, bodyHeight);
    this.relativeThrustDirection = relativeThrustDirection.normalize();

  }

  public void setThrustDirection(Vector relativeThrustDirection) {
    this.relativeThrustDirection = relativeThrustDirection;
  }

  @Override
  public Vector getThrustDirection() {
    return relativeThrustDirection.rotate(getStaticState().getAngularPosition());
  }
}
