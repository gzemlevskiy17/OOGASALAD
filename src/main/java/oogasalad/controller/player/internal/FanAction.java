package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;

/**
 * Toggles a fan component on or off each time execute() is called.
 *
 * FanAction tracks its own active state. A drive-capable fan component can hold a reference to
 * this action and use {@link #isActive()} to decide whether to return a thrust vector or zero.
 */
public class FanAction implements Action {

  private boolean active = false;

  @Override
  public void execute() {
    active = !active;
  }

  /** @return true if the fan is currently on. */
  public boolean isActive() {
    return active;
  }
}
