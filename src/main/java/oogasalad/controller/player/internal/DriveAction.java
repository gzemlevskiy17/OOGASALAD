package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;

/**
 * Toggles an engine/motor drive component on or off each time execute() is called.
 *
 * The DriveComponent should hold info ab this action and call isActive()
 * holds a reference to this action and calls isActive() inside getForce()
 * to decide whether to return a drive force or zero.
 */
public class DriveAction implements Action {

  private boolean active = false;

  @Override
  public void execute() {
    active = !active;
  }


  /** @return true if the drive component is currently on. */
  public boolean isActive() {
    return active;
  }
}
