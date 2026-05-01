package oogasalad.view.core.internal;

import java.util.Optional;
import oogasalad.model.engine.api.GameObject;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;

/**
 * Default Camera storing viewport rectangle, optional CameraBehavior, and visibility using point or AABB tests.
 *
 * @author Iurii Beliaev
 */
public class CameraImpl implements Camera {

  private double x;
  private double y;
  private double viewWidth;
  private double viewHeight;
  private CameraBehavior cameraBehavior;

  /**
   * Sets initial top-left position and positive viewport dimensions.
   */
  public CameraImpl(double x, double y, double viewportWidth, double viewportHeight) {
    this.x = x;
    this.y = y;
    setViewBoundaries(viewportWidth, viewportHeight);
  }

  /** Current viewport left edge in world space. */
  public double getX() {
    return this.x;
  }

  /** Current viewport top edge in world space. */
  public double getY() {
    return this.y;
  }

  /** Width of the visible viewport in world units. */
  @Override
  public double getViewWidth() {
    return viewWidth;
  }

  /** Height of the visible viewport in world units. */
  @Override
  public double getViewHeight() {
    return viewHeight;
  }

  /** Sets the world-space origin of this viewport rectangle. */
  @Override
  public void setPosition(double x, double y) {
    this.x = x;
    this.y = y;
  }


  /** Replaces optional camera motion strategy invoked by updateBehavior. */
  @Override
  public void setBehavior(CameraBehavior behavior) {
    this.cameraBehavior = behavior;
  }

  /** Runs the attached behavior once if present. */
  public void updateBehavior() {
    Optional.ofNullable(cameraBehavior).ifPresent(cb -> cb.update(this));
  }

  /** Replaces viewport extents; throws when width or height are not positive. */
  public void setViewBoundaries(double width, double height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Viewport size must be positive");
    }
    this.viewWidth = width;
    this.viewHeight = height;
  }

  /**
   * True when the renderable overlaps the viewport, using polygon bounds when the object is a GameObject. Determines if the object will be rendered
   */
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
