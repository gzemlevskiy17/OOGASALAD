package oogasalad.view.core.internal;

import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;

public class AbstractCameraBehavior implements CameraBehavior {

  private final double smoothing;
  private final boolean clampEnabled;
  private final double minX;
  private final double maxX;
  private final double minY;
  private final double maxY;


  public AbstractCameraBehavior() {
    this(1.0); // Set default for no smoothing
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

}
