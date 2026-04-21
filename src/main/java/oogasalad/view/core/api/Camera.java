package oogasalad.view.core.api;

/**
 * Viewport abstraction responsible for world visibility and camera movement state.
 *
 * @author Iurii Beliaev
 */
public interface Camera {

  /** @return true if the object is within the camera's current bounds. */
  boolean isVisible(Renderable object);

  double getX();
  double getY();
  double getViewWidth();
  double getViewHeight();

  void setPosition(double x, double y);

  void setViewBoundaries(double width, double height);

  /** @param behavior The logic that will control this camera's movement. */
  void setBehavior(CameraBehavior behavior);

  void updateBehavior();
}