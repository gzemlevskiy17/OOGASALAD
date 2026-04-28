package oogasalad.view.core.internal;

import java.util.Optional;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;

/**
 * Default camera implementation that stores viewport position/size, performs simple visibility
 * culling, and delegates motion updates to an optional {@link CameraBehavior}.
 *
 * @author Iurii Beliaev
 */
public class CameraImpl implements Camera {

  private double x; // viewport left in world coordinates
  private double y; // viewport top in world coordinates
  private double viewWidth;
  private double viewHeight;
  private CameraBehavior cameraBehavior;

  public CameraImpl(double x, double y, double viewportWidth, double viewportHeight) {
    this.x = x;
    this.y = y;
    setViewBoundaries(viewportWidth, viewportHeight);
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  @Override
  public double getViewWidth() {
    return viewWidth;
  }

  @Override
  public double getViewHeight() {
    return viewHeight;
  }

  @Override public void setPosition(double x, double y) {
    this.x = x;
    this.y = y;
  }


  @Override
  public void setBehavior(CameraBehavior behavior) {
    this.cameraBehavior = behavior;
  }

  public void updateBehavior() {
    Optional.ofNullable(cameraBehavior).ifPresent(cb -> cb.update(this));
  }

  public void setViewBoundaries(double width, double height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Viewport size must be positive");
    }
    this.viewWidth = width;
    this.viewHeight = height;
  }

  @Override
  public boolean isVisible(Renderable object) {
    if (object == null) {
      return false;
    }
    double viewLeft = x;
    double viewTop = y;
    double viewRight = x + viewWidth;
    double viewBottom = y + viewHeight;

    if (!(object instanceof GameObject gameObject)) {
      return pointInsideViewport(viewLeft, viewTop, viewRight, viewBottom,
          object.x(), object.y());
    }

    CollisionPolygonBounds.UnionAabb aabb = CollisionPolygonBounds.unionAabb(gameObject);
    return aabb.defined()
        ? CollisionPolygonBounds.rectsOverlap(
        aabb.minX(), aabb.minY(), aabb.maxX(), aabb.maxY(),
        viewLeft, viewTop, viewRight, viewBottom)
        : pointInsideViewport(viewLeft, viewTop, viewRight, viewBottom,
            object.x(), object.y());
  }

  private static boolean pointInsideViewport(double viewLeft, double viewTop,
      double viewRight, double viewBottom, double px, double py) {
    return px >= viewLeft && px <= viewRight && py >= viewTop && py <= viewBottom;
  }
}