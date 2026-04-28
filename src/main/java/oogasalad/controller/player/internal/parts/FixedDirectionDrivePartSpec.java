package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.FixedDirectionDriveComponent;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Palette specification for a drive whose thrust direction stays fixed in world coordinates.
 *
 * <p>This is the right choice for parts such as balloons, where the thrust should always point in
 * the same absolute direction regardless of the vehicle's rotation. It depends on the shared drive
 * metadata in {@link DrivePartSpec} and creates a {@link FixedDirectionDriveComponent} when the
 * vehicle is assembled.
 *
 * @author Georgiy Zemlevskiy
 */
public class FixedDirectionDrivePartSpec extends DrivePartSpec {

  public FixedDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, thrustDirection, thrustForce);
  }

  public FixedDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, thrustDirection, thrustForce, mass, width, height);
  }


  /**
   * Builds a drive component whose thrust direction is fixed in world space.
   *
   * @param x x-coordinate of the component's placement
   * @param y y-coordinate of the component's placement
   * @return a rigid body that also implements the drive contract
   */
  @Override
  public RigidBody create(double x, double y) {
    DriveComponent drive = new FixedDirectionDriveComponent(
        x, y, 0, getMass(), getFriction(), getMaxHP(), paletteName(), bodyWidth(), bodyHeight(), getThrustDirection());
    drive.setThrustForce(getThrustForce());
    return drive;
  }
}
