package oogasalad.view.core.internal;

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

  @Override public void setPosition(double x, double y) {
    this.x = x;
    this.y = y;
  }


  @Override
  public void setBehavior(CameraBehavior behavior) {
    this.cameraBehavior = behavior;
  }

  public void updateBehavior() {
    if (cameraBehavior != null) {
      cameraBehavior.update(this);
    }
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
    if (object == null) return false;

    double left = x;
    double right = x + viewWidth;
    double top = y;
    double bottom = y + viewHeight;

    if (object instanceof TerrainBlock tb) {
      double objLeft = tb.getX();
      double objRight = tb.getX() + tb.getWidth();
      double objTop = tb.getY();
      double objBottom = tb.getY() + tb.getHeight();

      return objRight >= left && objLeft <= right
          && objBottom >= top && objTop <= bottom;
    }

    double objectX = object.getX();
    double objectY = object.getY();
    return objectX >= left && objectX <= right && objectY >= top && objectY <= bottom;
  }
}