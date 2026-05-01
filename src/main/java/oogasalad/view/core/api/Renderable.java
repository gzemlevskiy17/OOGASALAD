package oogasalad.view.core.api;

/**
 * Read-only snapshot of position and orientation for rendering. Implementations usually mirror model
 * bodies without mutating game state inside the draw pass.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public interface Renderable {

  /** Horizontal world coordinate. */
  double x();

  /** Vertical world coordinate. */
  double y();

  /** Rotation in radians, aligned with engine angular state applied to sprites when enabled. */
  double rotation();
}
