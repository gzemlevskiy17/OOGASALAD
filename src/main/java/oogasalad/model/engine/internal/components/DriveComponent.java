package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.Vector;

/**
 * A vehicle component that generates thrust when enabled. Implements {@link Driveable} so the
 * engine can apply its force each frame when the player activates it.
 *
 * @author Paulina Vvedenskaya
 * @author Georgiy Zemlevskiy
 */
public abstract class DriveComponent extends VehicleComponent implements Driveable {

  private static final double DEFAULT_THRUST_FORCE = 3000.0; //TODO: properties
  private static final double DEFAULT_RESTITUTION = 0.5;

  private double thrustForce;
  private boolean driveEnabled;
  private boolean playerControllable;

  public DriveComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp, String displayName, double bodyWidth, double bodyHeight) {
    double momentOfInertia = mass * (bodyWidth * bodyWidth + bodyHeight * bodyHeight) / 12.0;
    ConvexPolygon polygon = ConvexPolygon.rotatedBox(x, y, bodyWidth, bodyHeight, rotation);
    super(
        x,
        y,
        rotation,
        polygon,
        "",
        friction,
        mass,
        momentOfInertia,
        DEFAULT_RESTITUTION,
        true,
        maxHp,
        displayName,
        bodyWidth,
        bodyHeight);
    this.thrustForce = DEFAULT_THRUST_FORCE;
    this.driveEnabled = false;
    this.playerControllable = true;
  }

//  @Override
//  public void setThrustDirection(Vector thrustDirection) {
//    this.thrustDirection = thrustDirection;
//  }

  @Override
  public abstract Vector getThrustDirection();

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
