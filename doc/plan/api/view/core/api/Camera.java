package oogasalad.view.core.api;

/** Determines visibility and manages the viewport state. */
public interface Camera {
  /** @return true if the object is within the camera's current bounds. */
  boolean isVisible(Renderable object);

  double getX();
  double getY();
  void setPosition(double x, double y);

  /** @param behavior The logic that will control this camera's movement. */
  void setBehavior(CameraBehavior behavior);
}