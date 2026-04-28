package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.Vector;

/**
 * A vehicle component that generates thrust when enabled. Implements {@link Driveable} so the
 * engine can apply its force each frame when the player activates it.
 *
 * @author Paulina Vvedenskaya
 */
public class DriveComponent extends VehicleComponent implements Driveable {

  private static final double DEFAULT_THRUST_FORCE = 3000.0;

  private Vector thrustDirection;
  private double thrustForce;
  private boolean driveEnabled;
  private boolean playerControllable;

  private static final double DEFAULT_RESTITUTION = 0.5;

  public DriveComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp, String displayName, double bodyWidth, double bodyHeight) {
    super(
        x,
        y,
        rotation,
        ConvexPolygon.rotatedBox(x, y, bodyWidth, bodyHeight, rotation),
        "",
        friction,
        mass,
        mass * (bodyWidth * bodyWidth + bodyHeight * bodyHeight) / 12.0,
        DEFAULT_RESTITUTION,
        true,
        maxHp,
        displayName,
        bodyWidth,
        bodyHeight);
    this.thrustDirection = new Vector(1, 0);
    this.thrustForce = DEFAULT_THRUST_FORCE;
    this.driveEnabled = false;
    this.playerControllable = true;
  }

  @Override
  public Vector getThrustDirection() {
    return thrustDirection;
  }

  @Override
  public void setThrustDirection(Vector thrustDirection) {
    this.thrustDirection = thrustDirection;
  }

  @Override
  public double getThrustForce() {
    return thrustForce;
  }

  @Override
  public void setThrustForce(double thrustForce) {
    this.thrustForce = thrustForce;
  }

  @Override
  public boolean isDriveEnabled() {
    return driveEnabled;
  }

  @Override
  public void setDriveEnabled(boolean driveEnabled) {
    this.driveEnabled = driveEnabled;
  }

  @Override
  public boolean isPlayerControllable() {
    return playerControllable;
  }

  @Override
  public void setPlayerControllable(boolean playerControllable) {
    this.playerControllable = playerControllable;
  }
}
