package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.Vector;

/**
 * Base class for rigid-body vehicle components that can generate thrust.
 *
 * <p>This class owns the shared drive state used by the engine and controller layers: thrust
 * magnitude, enabled/disabled state, and whether the player is allowed to toggle the drive. The
 * concrete subclasses supply the thrust direction policy — either fixed in world coordinates or
 * relative to the component's current rotation.
 *
 * <p>Assumptions: subclasses pass valid physical dimensions so the moment of inertia can be
 * computed, and callers only enable the drive when the component is meant to be active.
 *
 * <p>Example: instantiate a concrete subclass such as {@link FixedDirectionDriveComponent} and set
 * its thrust force before adding it to the world.
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

  /**
   * Creates a drive component with the shared rigid-body state used by all driveable parts.
   *
   * @param x x-coordinate of the component center
   * @param y y-coordinate of the component center
   * @param rotation initial rotation in radians
   * @param mass component mass used by physics and gravity
   * @param friction contact friction coefficient
   * @param maxHp maximum hit points before the component becomes inactive
   * @param displayName user-facing name used by the controller and palette
   * @param bodyWidth body width used for collision geometry and inertia calculation
   * @param bodyHeight body height used for collision geometry and inertia calculation
   */
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

  /**
   * Returns the current thrust direction used when computing drive force.
   *
   * <p>Subclasses define whether the returned vector is fixed in world space or rotated with the
   * component's current orientation.
   */
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
