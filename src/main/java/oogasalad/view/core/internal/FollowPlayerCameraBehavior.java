package oogasalad.view.core.internal;

import java.util.function.Supplier;
import oogasalad.view.core.api.AbstractCameraBehavior;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;

/**
 * Camera behavior that keeps the viewport centered on a renderable supplied each frame
 * (typically the player), with optional pixel offsets and smoothing from the superclass.
 * When finite world width and height are given, the desired camera position is clamped so the
 * viewport does not scroll past the level edges. If the supplier returns null, the camera
 * holds its current position until a target exists again.
 *
 * @author Iurii Beliaev
 */
public class FollowPlayerCameraBehavior extends AbstractCameraBehavior {
  private final Supplier<? extends Renderable> playerSupplier;
  private final double worldWidth;
  private final double worldHeight;
  private final double offsetX;
  private final double offsetY;

  public FollowPlayerCameraBehavior(Supplier<? extends Renderable> targetSupplier) {
    this(targetSupplier, 0.0, 0.0, 1.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public FollowPlayerCameraBehavior(
      Supplier<? extends Renderable> playerSupplier,
      double offsetX,
      double offsetY,
      double smoothing
  ) {
    this(playerSupplier, offsetX, offsetY, smoothing, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  /**
   * @param playerSupplier source of the follow target each update; must not be null
   * @param offsetX horizontal offset from strict centering of the target
   * @param offsetY vertical offset from strict centering of the target
   * @param smoothing interpolation factor in zero to one passthrough to AbstractCameraBehavior
   * @param worldWidth finite level width used to clamp horizontal scroll, or non-finite for no clamp
   * @param worldHeight finite level height used to clamp vertical scroll, or non-finite for no clamp
   */
  public FollowPlayerCameraBehavior(
      Supplier<? extends Renderable> playerSupplier,
      double offsetX,
      double offsetY,
      double smoothing,
      double worldWidth,
      double worldHeight
  ) {
    super(smoothing);

    if (playerSupplier == null) {
      throw new IllegalArgumentException("playerSupplier cannot be null");
    }

    this.playerSupplier = playerSupplier;
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  @Override
  protected double desiredX(Camera camera) {
    Renderable player = playerSupplier.get();
    if (player == null) {
      return camera.getX();
    }
    double desired = player.x() - camera.getViewWidth() / 2.0 + offsetX;
    return clampToWorld(desired, camera.getViewWidth(), worldWidth);
  }

  @Override
  protected double desiredY(Camera camera) {
    Renderable player = playerSupplier.get();
    if (player == null) {
      return camera.getY();
    }
    double desired = player.y() - camera.getViewHeight() / 2.0 + offsetY;
    return clampToWorld(desired, camera.getViewHeight(), worldHeight);
  }

  private double clampToWorld(double desired, double viewportExtent, double worldExtent) {
    if (!Double.isFinite(worldExtent) || worldExtent <= 0) {
      return desired;
    }
    double maxAlongAxis = Math.max(0.0, worldExtent - viewportExtent);
    return clamp(desired, 0.0, maxAlongAxis);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

}
