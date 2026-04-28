package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Vector;

/**
 * Drive component whose thrust direction stays fixed in world coordinates.
 *
 * <p>Use this for components like balloons where the thrust should always point in the same global
 * direction regardless of the vehicle's rotation. The constructor normalizes the supplied vector so
 * the drive stores a direction rather than an arbitrary magnitude.
 *
 * @author Georgiy Zemlevskiy
 */
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
