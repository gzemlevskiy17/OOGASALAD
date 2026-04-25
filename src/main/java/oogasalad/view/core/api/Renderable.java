package oogasalad.view.core.api;

/** Provides read-only graphical data for rendering. */
public interface Renderable {
  double getX();
  double getY();

  /**
   * @return rotation in radians, matching the physics engine's angular state
   */
  double getRotation();
}