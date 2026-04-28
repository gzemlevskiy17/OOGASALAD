package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.RelativeDirectionDriveComponent;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Palette specification for a drive whose thrust follows the vehicle's current rotation.
 *
 * <p>Use this for engines and fans that should keep pushing relative to the car body rather than
 * in a fixed world direction. It reuses the shared drive metadata from {@link DrivePartSpec} and
 * produces a {@link RelativeDirectionDriveComponent} at build time.
 *
 * @author Georgiy Zemlevskiy
 */
public class RelativeDirectionDrivePartSpec extends DrivePartSpec {

  public RelativeDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, thrustDirection, thrustForce);
  }

  public RelativeDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, thrustDirection, thrustForce, mass, width, height);
  }


  /**
   * Builds a drive component whose thrust rotates with the component's current angle.
   *
   * @param x x-coordinate of the component's placement
   * @param y y-coordinate of the component's placement
   * @return a rigid body that also implements the drive contract
   */
  @Override
  public RigidBody create(double x, double y) {
    DriveComponent drive = new RelativeDirectionDriveComponent(
        x, y, 0, getMass(), getFriction(), getMaxHP(), paletteName(), bodyWidth(), bodyHeight(), getThrustDirection());
    drive.setThrustForce(getThrustForce());
    return drive;
  }
}
