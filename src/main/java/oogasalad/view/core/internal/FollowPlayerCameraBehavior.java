package oogasalad.view.core.internal;

import java.util.function.Supplier;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;

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
      return camera.getX(); // keep current position if no player available
    }

    double desired = player.x() - camera.getViewWidth() / 2.0 + offsetX;
    return clampToWorldX(desired, camera.getViewWidth());
  }

  @Override
  protected double desiredY(Camera camera) {
    Renderable player = playerSupplier.get();

    if (player == null) {
      return camera.getY();// keep current position if no player available
    }

    double desired = player.y() - camera.getViewHeight() / 2.0 + offsetY;
    return clampToWorldY(desired, camera.getViewHeight());
  }

  private double clampToWorldX(double desired, double viewportWidth) {
    if (!Double.isFinite(worldWidth) || worldWidth <= 0) {
      return desired;
    }
    double maxX = Math.max(0, worldWidth - viewportWidth);
    return clamp(desired, 0, maxX);
  }

  private double clampToWorldY(double desired, double viewportHeight) {
    if (!Double.isFinite(worldHeight) || worldHeight <= 0) {
      return desired;
    }
    double maxY = Math.max(0, worldHeight - viewportHeight);
    return clamp(desired, 0, maxY);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

}
