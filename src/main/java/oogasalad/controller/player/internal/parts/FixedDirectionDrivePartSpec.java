package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.FixedDirectionDriveComponent;
import oogasalad.model.engine.internal.components.RigidBody;

public class FixedDirectionDrivePartSpec extends DrivePartSpec {

  public FixedDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, thrustDirection, thrustForce);
  }

  public FixedDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, thrustDirection, thrustForce, mass, width, height);
  }


  @Override
  public RigidBody create(double x, double y) {
    DriveComponent drive = new FixedDirectionDriveComponent(
        x, y, 0, getMass(), getFriction(), getMaxHP(), paletteName(), bodyWidth(), bodyHeight(), getThrustDirection());
    drive.setThrustForce(getThrustForce());
    return drive;
  }
}
