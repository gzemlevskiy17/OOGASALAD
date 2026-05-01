package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;
import oogasalad.model.engine.api.Driveable;

/**
 * Toggles a {@link Driveable}'s drive-enabled flag each time {@link #execute()} is called.
 *
 * The physics engine reads the flag each step via {@link Driveable#getDriveForce()} and
 * returns thrust while enabled, zero otherwise.
 */
public class DriveAction implements Action {

  private final Driveable target;

  public DriveAction(Driveable target) {
    this.target = target;
  }

  @Override
  public void execute() {
    if (target.isDriveEnabled()) {
      target.deactivateDrive();
    } else {
      target.activateDrive();
    }
  }
}
