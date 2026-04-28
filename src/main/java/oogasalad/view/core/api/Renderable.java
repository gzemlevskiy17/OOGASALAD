package oogasalad.view.core.api;

/** Provides read-only graphical data for rendering. */
public interface Renderable {
  double x();
  double y();

  /**
   * @return rotation in radians, matching the physics engine's angular state
   */
  double rotation();
}