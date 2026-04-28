package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.RelativeDirectionDriveComponent;
import oogasalad.model.engine.internal.components.RigidBody;

public class RelativeDirectionDrivePartSpec extends DrivePartSpec {

  public RelativeDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, thrustDirection, thrustForce);
  }

  public RelativeDirectionDrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, thrustDirection, thrustForce, mass, width, height);
  }


  @Override
  public RigidBody create(double x, double y) {
    DriveComponent drive = new RelativeDirectionDriveComponent(
        x, y, 0, getMass(), getFriction(), getMaxHP(), paletteName(), bodyWidth(), bodyHeight(), getThrustDirection());
    drive.setThrustForce(getThrustForce());
    return drive;
  }
}
