package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;

/**
 * Toggles an engine/motor drive component on or off each time execute() is called.
 *
 * A drive-capable component can hold a reference to this action and use {@link #isActive()} to
 * decide whether to output thrust or zero force.
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
