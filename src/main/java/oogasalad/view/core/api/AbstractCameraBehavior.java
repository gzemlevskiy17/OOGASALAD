package oogasalad.view.core.api;

/**
 * Template for smoothed camera motion: subclasses supply desired world coordinates; this class lerps toward
 * them and optionally clamps to an axis-aligned rectangle.
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


  /** Equivalent to smoothing factor 1.0 with clamping disabled. */
  public AbstractCameraBehavior() {
    this(1.0);
  }

  /**
   * Smoothing-only constructor; subclass desired positions may still be constrained elsewhere.
   */
  public AbstractCameraBehavior(double smoothing) {
    this(smoothing, false, 0, 0, 0, 0);
  }

  /**
   * Full constructor controlling interpolation weight and optional axis-aligned clamp box.
   */
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

  /**
   * Interpolates the camera toward subclass-provided targets, optionally clamping inside configured bounds.
   */
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

  /** Desired camera X before smoothing and subclass-independent clamp aggregation. */
  protected abstract double desiredX(Camera camera);

  /** Desired camera Y before smoothing and subclass-independent clamp aggregation. */
  protected abstract double desiredY(Camera camera);

  private static double lerp(double from, double to, double t) {
    return from + (to - from) * t;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

}
