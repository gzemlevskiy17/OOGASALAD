package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * @author Paulina Vvedenskaya
 */
public final class DrivePartSpec extends AbstractVehiclePartSpec {

  private static final double MASS = 5.0;
  private static final double FRICTION = 0.3;
  private static final double MAX_HP = 100.0;

  private final Vector thrustDirection;
  private final double thrustForce;

  public DrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, JointStyle.STRUCTURAL);
    this.thrustDirection = thrustDirection;
    this.thrustForce = thrustForce;
  }

  @Override
  public GameComponent create(double x, double y) {
    DriveComponent drive = new DriveComponent(
        x, y, 0, MASS, FRICTION, MAX_HP, paletteName(), bodyWidth(), bodyHeight());
    drive.setThrustDirection(thrustDirection);
    drive.setThrustForce(thrustForce);
    return drive;
  }
}
