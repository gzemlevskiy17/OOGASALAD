package oogasalad.view.core.internal;

import java.util.function.Supplier;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;

public class FollowPlayerCameraBehavior extends AbstractCameraBehavior {
  private final Supplier<? extends Renderable> playerSupplier;
  private final double viewWidth;
  private final double viewHeight;
  private final double offsetX;
  private final double offsetY;

  public FollowPlayerCameraBehavior(Supplier<? extends Renderable> targetSupplier, double viewWidth, double viewHeight) {
    this(targetSupplier, viewWidth, viewHeight, 0.0, 0.0, 1.0);
  }

  public FollowPlayerCameraBehavior(
      Supplier<? extends Renderable> playerSupplier,
      double viewWidth,
      double viewHeight,
      double offsetX,
      double offsetY,
      double smoothing
  ) {
    super(smoothing);

    if (playerSupplier == null) {
      throw new IllegalArgumentException("playerSupplier cannot be null");
    }

    if (viewWidth <= 0 || viewHeight <= 0) {
      throw new IllegalArgumentException("viewport dimensions must be positive");
    }

    this.playerSupplier = playerSupplier;
    this.viewWidth = viewWidth;
    this.viewHeight = viewHeight;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
  }

  @Override
  protected double desiredX(Camera camera) {
    Renderable player = playerSupplier.get();

    if (player == null) {
      return camera.getX(); // keep current position if no player available
    }

    return player.getX() - viewWidth / 2.0 + offsetX;
  }

  @Override
  protected double desiredY(Camera camera) {
    Renderable player = playerSupplier.get();

    if (player == null) {
      return camera.getY();// keep current position if no player available
    }

    return player.getY() - viewHeight / 2.0 + offsetY;
  }



}
