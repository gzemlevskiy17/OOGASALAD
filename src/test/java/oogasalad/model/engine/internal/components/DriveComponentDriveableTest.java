package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies DriveComponent's implementation of the Driveable contract and its interaction with the
 * lifecycle gate ({@code isActive()}) inherited from GameComponent — in particular, a destroyed
 * drive must produce zero thrust even if the player left it enabled.
 */
class FixedDirectionDriveComponentDriveableTest {

  private static final double EPS = 1e-9;
  private FixedDirectionDriveComponent drive;

  @BeforeEach
  void setUp() {
    drive = new FixedDirectionDriveComponent(0, 0, 0, 10.0, 0.3, 100.0, "Fan", 60.0, 60.0,
        new Vector(1, 0));
  }

  @Test
  void defaults_driveDisabled_and_playerControllable() {
    assertFalse(drive.isDriveEnabled(),
        "new drive should start off — player must opt in");
    assertTrue(drive.isPlayerControllable(),
        "default drives should be player-controllable");
  }

  @Test
  void getDriveForce_whenDisabled_returnsZero() {
    drive.setThrustDirection(new Vector(1, 0));
    drive.setThrustForce(500.0);

    Vector f = drive.getDriveForce();

    assertEquals(0.0, f.x(), EPS);
    assertEquals(0.0, f.y(), EPS);
  }

  @Test
  void getDriveForce_whenEnabledAndActive_returnsThrustDirectionTimesForce() {
    drive.setThrustDirection(new Vector(0, -1));
    drive.setThrustForce(250.0);
    drive.setDriveEnabled(true);

    Vector f = drive.getDriveForce();

    assertEquals(0.0, f.x(), EPS, "x-component should be direction.x * force");
    assertEquals(-250.0, f.y(), EPS, "y-component should be direction.y * force");
  }

  @Test
  void getDriveForce_whenDestroyed_returnsZero_evenIfEnabled() {
    drive.setThrustDirection(new Vector(1, 0));
    drive.setThrustForce(500.0);
    drive.setDriveEnabled(true);

    drive.damage(drive.getMaxHp()); // deactivates on reaching zero HP

    assertFalse(drive.isActive(), "destroyed component should report inactive");
    Vector f = drive.getDriveForce();
    assertEquals(0.0, f.x(), EPS,
        "a destroyed drive must not produce thrust regardless of driveEnabled");
    assertEquals(0.0, f.y(), EPS);
  }

  @Test
  void setThrustForce_reflectedInGetDriveForce() {
    drive.setThrustDirection(new Vector(1, 0));
    drive.setDriveEnabled(true);

    drive.setThrustForce(0.0);
    assertEquals(0.0, drive.getDriveForce().x(), EPS,
        "zero force should give zero thrust even when enabled");

    drive.setThrustForce(42.0);
    assertEquals(42.0, drive.getDriveForce().x(), EPS);
  }

  @Test
  void setThrustDirection_reflectedInGetDriveForce() {
    drive.setThrustForce(10.0);
    drive.setDriveEnabled(true);

    drive.setThrustDirection(new Vector(0, 1));
    Vector down = drive.getDriveForce();
    assertEquals(0.0, down.x(), EPS);
    assertEquals(10.0, down.y(), EPS);

    drive.setThrustDirection(new Vector(-1, 0));
    Vector left = drive.getDriveForce();
    assertEquals(-10.0, left.x(), EPS);
    assertEquals(0.0, left.y(), EPS);
  }
}
