package oogasalad.model.engine.api;

/**
 * Capability interface for components that can generate thrust when enabled.
 *
 * <p>Implementations provide storage for thrust settings and enabled state, while this interface
 * centralizes the common drive-force calculation and lifecycle toggles.
 */
public interface Driveable {

  Vector getThrustDirection();

//  void setThrustDirection(Vector thrustDirection);

  double getThrustForce();

  void setThrustForce(double thrustForce);

  boolean isDriveEnabled();

  void setDriveEnabled(boolean driveEnabled);

  boolean isPlayerControllable();

  void setPlayerControllable(boolean playerControllable);

  boolean isActive();

  default void activateDrive() {
    setDriveEnabled(true);
  }

  default void deactivateDrive() {
    setDriveEnabled(false);
  }

  default Vector getDriveForce() {
    if (!isActive() || !isDriveEnabled()) {
      return new Vector(0, 0);
    }
    return getThrustDirection().multiply(getThrustForce());
  }
}
