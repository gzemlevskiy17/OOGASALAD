package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.Test;

class FollowPlayerCameraBehaviorTest {

  private static final double EPS = 1e-9;

  // ── constructor validation ─────────────────────────────────────────────────

  private static Renderable makePlayer(double x, double y) {
    return new RenderableImpl(x, y, 0.0);
  }

  @Test
  void constructor_nullPlayerSupplier_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new FollowPlayerCameraBehavior(null, 100, 100));
  }

  @Test
  void constructor_zeroViewWidth_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 0, 100));
  }

  // ── centering logic (smoothing = 1.0 → instant movement) ──────────────────

  @Test
  void constructor_negativeViewHeight_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 100, -1));
  }

  @Test
  void update_centersOnPlayer_noOffset() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(200, 300), 100, 100, 0, 0, 1.0);

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
        new FollowPlayerCameraBehavior(() -> makePlayer(100, 100), 100, 100, 20, 0, 1.0);

    behavior.update(camera);

    // desired X = 100 - 50 + 20 = 70
    assertEquals(70.0, camera.getX(), EPS);
  }

  @Test
  void update_appliesYOffset() {
    CameraImpl camera = new CameraImpl(0, 0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(100, 100), 100, 100, 0, -15, 1.0);

    behavior.update(camera);

    // desired Y = 100 - 50 + (-15) = 35
    assertEquals(35.0, camera.getY(), EPS);
  }

  // ── null player ───────────────────────────────────────────────────────────

  @Test
  void update_playerAtOrigin_centersOnOrigin() {
    CameraImpl camera = new CameraImpl(999, 999, 200, 150);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 200, 150, 0, 0, 1.0);

    behavior.update(camera);

    assertEquals(-100.0, camera.getX(), EPS);  // 0 - 200/2 = -100
    assertEquals(-75.0, camera.getY(), EPS);   // 0 - 150/2 = -75
  }

  // ── smoothing ─────────────────────────────────────────────────────────────

  @Test
  void update_nullPlayer_keepsCameraAtCurrentPosition() {
    CameraImpl camera = new CameraImpl(42.0, 77.0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> null, 100, 100, 0, 0, 1.0);

    behavior.update(camera);

    assertEquals(42.0, camera.getX(), EPS);
    assertEquals(77.0, camera.getY(), EPS);
  }

  @Test
  void update_smoothingZero_cameraDoesNotMove() {
    // smoothing = 0 → lerp(from, to, 0) = from → camera never moves
    CameraImpl camera = new CameraImpl(10.0, 20.0, 100, 100);
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(500, 500), 100, 100, 0, 0, 0.0);

    behavior.update(camera);

    assertEquals(10.0, camera.getX(), EPS);
    assertEquals(20.0, camera.getY(), EPS);
  }

  // ── clamping (via parent constructor) ────────────────────────────────────

  @Test
  void update_smoothingHalf_cameraMidway() {
    // Camera at (0,0), target at (100,100) → after one step with t=0.5 → (50,50)
    CameraImpl camera = new CameraImpl(0.0, 0.0, 100, 100);
    // Player at (150,150) → desired = 150 - 50 = 100 on each axis
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(150, 150), 100, 100, 0, 0, 0.5);

    behavior.update(camera);

    // lerp(0, 100, 0.5) = 50
    assertEquals(50.0, camera.getX(), EPS);
    assertEquals(50.0, camera.getY(), EPS);
  }

  @Test
  void abstractBehavior_invalidClampBounds_throws() {
    // minX > maxX → should throw on construction
    assertThrows(IllegalArgumentException.class, () ->
        // Use an anonymous subclass to test AbstractCameraBehavior clamp validation directly
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

  // ── helper ───────────────────────────────────────────────────────────────

  @Test
  void abstractBehavior_nullCamera_throws() {
    FollowPlayerCameraBehavior behavior =
        new FollowPlayerCameraBehavior(() -> makePlayer(0, 0), 100, 100);
    assertThrows(IllegalArgumentException.class, () -> behavior.update(null));
  }
}
