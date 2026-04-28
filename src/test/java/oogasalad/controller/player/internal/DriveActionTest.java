package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

/**
 * Pure toggle-semantics tests for {@link DriveAction}, using a minimal in-memory Driveable fake so
 * the tests don't depend on DriveComponent, GameComponent, or the physics stack.
 */
class DriveActionTest {

  @Test
  void execute_fromDefaultDisabled_enablesDrive() {
    FakeDriveable target = new FakeDriveable();
    new DriveAction(target).execute();

    assertTrue(target.isDriveEnabled(),
        "first execute() on an off drive should enable it");
  }

  @Test
  void execute_twice_returnsToDisabled() {
    FakeDriveable target = new FakeDriveable();
    DriveAction action = new DriveAction(target);

    action.execute();
    action.execute();

    assertFalse(target.isDriveEnabled(),
        "two toggles should cancel back to disabled");
  }

  @Test
  void execute_threeTimes_endsEnabled() {
    FakeDriveable target = new FakeDriveable();
    DriveAction action = new DriveAction(target);

    action.execute();
    action.execute();
    action.execute();

    assertTrue(target.isDriveEnabled(),
        "odd number of toggles should leave drive enabled");
  }

  @Test
  void execute_whenTargetAlreadyEnabled_disables() {
    FakeDriveable target = new FakeDriveable();
    target.setDriveEnabled(true);

    new DriveAction(target).execute();

    assertFalse(target.isDriveEnabled(),
        "toggle from enabled should disable — action respects prior state");
  }

  @Test
  void execute_onlyAffectsBoundTarget() {
    FakeDriveable a = new FakeDriveable();
    FakeDriveable b = new FakeDriveable();
    DriveAction actionA = new DriveAction(a);

    actionA.execute();

    assertTrue(a.isDriveEnabled(), "bound target should toggle");
    assertFalse(b.isDriveEnabled(), "unbound target must not be touched");
  }

  private static final class FakeDriveable implements Driveable {

    private final Vector thrustDirection = new Vector(1, 0);
    private double thrustForce = 1.0;
    private boolean driveEnabled = false;
    private boolean playerControllable = true;
    private boolean active = true;

    @Override
    public Vector getThrustDirection() {
      return thrustDirection;
    }


    @Override
    public double getThrustForce() {
      return thrustForce;
    }

    @Override
    public void setThrustForce(double f) {
      this.thrustForce = f;
    }

    @Override
    public boolean isDriveEnabled() {
      return driveEnabled;
    }

    @Override
    public void setDriveEnabled(boolean v) {
      this.driveEnabled = v;
    }

    @Override
    public boolean isPlayerControllable() {
      return playerControllable;
    }

    @Override
    public void setPlayerControllable(boolean v) {
      this.playerControllable = v;
    }

    @Override
    public boolean isActive() {
      return active;
    }

    void setActive(boolean v) {
      this.active = v;
    }
  }
}
