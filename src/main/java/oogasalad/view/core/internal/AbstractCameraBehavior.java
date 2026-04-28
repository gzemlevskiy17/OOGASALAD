package oogasalad.view.core.internal;

import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;

/**
 * Default structure for camera motion: each update reads a desired position from subclasses,
 * optionally clamps that position to an axis-aligned box, then moves the camera toward the
 * target by linear interpolation. Smoothing is a zero-to-one blend factor: one means jump
 * immediately to the target, zero would leave the camera frozen (not recommended at exactly zero).
 *
 * @author Iurii Beliaev
 */
public abstract class AbstractCameraBehavior implements CameraBehavior {

  private final double smoothing;
  private final boolean clampEnabled;
  private final double minX;
  private final double maxX;
  private final double minY;
  private final double maxY;


  public AbstractCameraBehavior() {
    this(1.0);
  }

  public AbstractCameraBehavior(double smoothing) {
    this(smoothing, false, 0, 0, 0, 0);
  }

  public AbstractCameraBehavior(double smoothing, boolean clampEnabled, double minX, double maxX, double minY, double maxY) {
    if (smoothing < 0.0 || smoothing > 1.0) {
      throw new IllegalArgumentException("smoothing must be in [0, 1]");
    }

    if (clampEnabled && (minX > maxX || minY > maxY)) {
      throw new IllegalArgumentException("Invalid clamp bounds");
    }

    this.smoothing = smoothing;
    this.clampEnabled = clampEnabled;
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
  }

  @Override
  public void update(Camera camera) {
    if (camera == null) {
      throw new IllegalArgumentException("Camera cannot be null");
    }

    double targetX = desiredX(camera);
    double targetY = desiredY(camera);

    if (clampEnabled) {
      targetX = clamp(targetX, minX, maxX);
      targetY = clamp(targetY, minY, maxY);
    }

    double nextX = lerp(camera.getX(), targetX, smoothing);
    double nextY = lerp(camera.getY(), targetY, smoothing);

    camera.setPosition(nextX, nextY);
  }

  /** World X coordinate the camera should aim for before smoothing and optional bounds clamp. */
  protected abstract double desiredX(Camera camera);

  /** World Y coordinate the camera should aim for before smoothing and optional bounds clamp. */
  protected abstract double desiredY(Camera camera);

  // Linear interpolation to integrate smooting
  private static double lerp(double from, double to, double t) {
    return from + (to - from) * t;
  }
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

}
