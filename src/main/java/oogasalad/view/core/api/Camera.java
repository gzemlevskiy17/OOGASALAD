package oogasalad.view.core.api;

/**
 * Viewport abstraction for world-space visibility checks and viewport position and size used by Renderer.
 *
 * @author Iurii Beliaev
 */
public interface Camera {

  /** * True when the renderable overlaps the viewport, using polygon bounds when the object is a GameObject. Determines if the object will be rendered*/
  boolean isVisible(Renderable object);

  /** World-space X coordinate of the viewport origin (typically top-left). */
  double getX();

  /** World-space Y coordinate of the viewport origin (typically top-left). */
  double getY();

  /** Visible width span in world units. */
  double getViewWidth();

  /** Visible height span in world units. */
  double getViewHeight();

  /** Sets the viewport origin in world coordinates. */
  void setPosition(double x, double y);

  /** Defines viewport width and height; implementations should reject non-positive extents. */
  void setViewBoundaries(double width, double height);

  /** Assigns optional movement strategy invoked from updateBehavior. */
  void setBehavior(CameraBehavior behavior);

  /** Advances the attached CameraBehavior by one tick, if any. */
  void updateBehavior();
}
