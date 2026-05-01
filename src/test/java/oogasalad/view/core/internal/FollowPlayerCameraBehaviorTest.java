package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.view.core.api.AbstractCameraBehavior;
import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.Test;

/** Unit tests for FollowPlayerCameraBehavior centering, offsets, smoothing, clamps, and validation. */
class FollowPlayerCameraBehaviorTest {

  private static final double EPS = 1e-9;

  /** Minimal RenderableImpl stand-in for simulated player coordinates. */
  private static Renderable makePlayer(double x, double y) {
    return new RenderableImpl(x, y, 0.0);
  }

  @Test
  void constructor_nullPlayerSupplier_throws() {
    assertThrows(IllegalArgumentException.class, () -> new FollowPlayerCameraBehavior(null));
  }

  @Test
  void constructor_nullPlayerSupplier_withOffsetsAndSmoothing_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new FollowPlayerCameraBehavior(null, 0, 0, 1.0));
  }

  // ── centering logic (smoothing = 1.0 → instant movement) ──────────────────

  @Test
  void update_centersOnPlayer_noOffset() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(200, 300), 0, 0, 1.0);

    behavior.update(camera);

    // desired X = player.x - viewWidth/2 + offsetX = 200 - 50 + 0 = 150
    assertEquals(150.0, camera.getX(), EPS);
    // desired Y = 300 - 50 = 250
    assertEquals(250.0, camera.getY(), EPS);
  }

  @Test
  void update_appliesXOffset() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(100, 100), 20, 0, 1.0);

    behavior.update(camera);

    // desired X = 100 - 50 + 20 = 70
    assertEquals(70.0, camera.getX(), EPS);
  }

  @Test
  void update_appliesYOffset() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(100, 100), 0, -15, 1.0);

    behavior.update(camera);

    // desired Y = 100 - 50 + (-15) = 35
    assertEquals(35.0, camera.getY(), EPS);
  }

  @Test
  void update_playerAtOrigin_centersOnOrigin() {
    CameraImpl camera = new CameraImpl(999, 999, 200, 150);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 0, 0, 1.0);

    behavior.update(camera);

    assertEquals(-100.0, camera.getX(), EPS);  // 0 - 200/2 = -100
    assertEquals(-75.0, camera.getY(), EPS);   // 0 - 150/2 = -75
  }

  // ── null player ───────────────────────────────────────────────────────────

  @Test
  void update_nullPlayer_keepsCameraAtCurrentPosition() {
    CameraImpl camera = new CameraImpl(42.0, 77.0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> null, 0, 0, 1.0);

    behavior.update(camera);

    assertEquals(42.0, camera.getX(), EPS);
    assertEquals(77.0, camera.getY(), EPS);
  }

  // ── smoothing ─────────────────────────────────────────────────────────────

  @Test
  void update_smoothingZero_cameraDoesNotMove() {
    CameraImpl camera = new CameraImpl(10.0, 20.0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(500, 500), 0, 0, 0.0);

    behavior.update(camera);

    assertEquals(10.0, camera.getX(), EPS);
    assertEquals(20.0, camera.getY(), EPS);
  }

  @Test
  void update_smoothingHalf_cameraMidway() {
    CameraImpl camera = new CameraImpl(0.0, 0.0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(150, 150), 0, 0, 0.5);

    behavior.update(camera);

    // desired = 150 - 50 = 100 on each axis; lerp(0, 100, 0.5) = 50
    assertEquals(50.0, camera.getX(), EPS);
    assertEquals(50.0, camera.getY(), EPS);
  }

  // ── world clamping (finite world width/height on behavior) ─────────────────

  @Test
  void update_finiteWorld_clampsDesiredXToMaxScroll() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior = new FollowPlayerCameraBehavior(
        () -> makePlayer(300, 100), 0, 0, 1.0, 200, Double.POSITIVE_INFINITY);

    behavior.update(camera);

    // unclamped desired X = 250; maxX = 200 - 100 = 100
    assertEquals(100.0, camera.getX(), EPS);
    assertEquals(50.0, camera.getY(), EPS);
  }

  @Test
  void update_finiteWorld_clampsDesiredToZeroWhenPlayerWouldLeaveTopLeft() {
    CameraImpl camera = new CameraImpl(50, 50, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 0, 0, 1.0, 200, 200);

    behavior.update(camera);

    assertEquals(0.0, camera.getX(), EPS);
    assertEquals(0.0, camera.getY(), EPS);
  }

  // ── AbstractCameraBehavior validation (base class, not FollowPlayer-specific)

  @Test
  void abstractBehavior_invalidClampBounds_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        new AbstractCameraBehavior(1.0, true, 100, 50, 0, 100) {
          @Override
          protected double desiredX(oogasalad.view.core.api.Camera c) {
            return 0;
          }

          @Override
          protected double desiredY(oogasalad.view.core.api.Camera c) {
            return 0;
          }
        });
  }

  @Test
  void abstractBehavior_invalidSmoothingAboveOne_throws() {
    assertThrows(IllegalArgumentException.class, () ->
        new AbstractCameraBehavior(1.5) {
          @Override
          protected double desiredX(oogasalad.view.core.api.Camera c) {
            return 0;
          }

          @Override
          protected double desiredY(oogasalad.view.core.api.Camera c) {
            return 0;
          }
        });
  }

  @Test
  void abstractBehavior_negativeSmoothingThrows() {
    assertThrows(IllegalArgumentException.class, () ->
        new AbstractCameraBehavior(-0.1) {
          @Override
          protected double desiredX(oogasalad.view.core.api.Camera c) {
            return 0;
          }

          @Override
          protected double desiredY(oogasalad.view.core.api.Camera c) {
            return 0;
          }
        });
  }

  @Test
  void abstractBehavior_nullCamera_throws() {
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(0, 0));
    assertThrows(IllegalArgumentException.class, () -> behavior.update(null));
  }
}