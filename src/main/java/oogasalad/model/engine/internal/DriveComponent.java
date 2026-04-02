package oogasalad.model.engine.internal;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;

public abstract class DriveComponent extends VehicleComponent {
  private Vector thrustDirection;
  private double thrustForce;
  private boolean driveEnabled;
  private boolean playerControllable;

  public DriveComponent(Pose pose, double mass, double friction, double maxHp,
      Vector thrustDirection, double thrustForce, boolean playerControllable) {
    super(pose, mass, friction, maxHp);
    setThrustDirection(thrustDirection);
    setThrustForce(thrustForce);
    this.playerControllable = playerControllable;
    this.driveEnabled = false;
  }

  public Vector getThrustDirection() {
    return thrustDirection;
  }

  public void setThrustDirection(Vector thrustDirection) {
    this.thrustDirection = java.util.Objects.requireNonNull(thrustDirection, EngineError.NULL_VECTOR.message());
  }

  public double getThrustForce() {
    return thrustForce;
  }

  public void setThrustForce(double thrustForce) {
    if (thrustForce < 0) {
      throw new IllegalArgumentException(EngineError.THRUST_FORCE_MUST_BE_NON_NEGATIVE.message());
    }
    this.thrustForce = thrustForce;
  }

  public boolean isDriveEnabled() {
    return driveEnabled;
  }

  public void activateDrive() {
    driveEnabled = true;
  }

  public void deactivateDrive() {
    driveEnabled = false;
  }

  public boolean isPlayerControllable() {
    return playerControllable;
  }

  public void setPlayerControllable(boolean playerControllable) {
    this.playerControllable = playerControllable;
  }

  public Vector getDriveForce() {
    if (!isActive() || !driveEnabled) {
      return new Vector(0, 0);
    }
    return thrustDirection.multiply(thrustForce);
  }

  @Override
  public Vector getForce() {
    return super.getForce().add(getDriveForce());
  }

  @Override
  public void activate() {
    super.activate();
    activateDrive();
  }

  @Override
  public void deactivate() {
    super.deactivate();
    deactivateDrive();
  }

  @Override
  public void destroy() {
    deactivateDrive();
    super.destroy();
  }
}
