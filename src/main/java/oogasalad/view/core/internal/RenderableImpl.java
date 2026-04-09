package oogasalad.view.core.internal;

import oogasalad.view.core.api.Renderable;

/**
 * Immutable value implementation of {@link Renderable}.
 *
 * @author Iurii Beliaev
 */
public class RenderableImpl implements Renderable {

  private final double x;
  private final double y;
  private final double rotation;

  /**
   * Creates a renderable snapshot.
   *
   * @param x world x-coordinate
   * @param y world y-coordinate
   * @param rotation rotation in degrees
   */
  public RenderableImpl(double x, double y, double rotation) {
    this.x = x;
    this.y = y;
    this.rotation = rotation;
  }

  @Override
  public double getX() {
    return x;
  }

  @Override
  public double getY() {
    return y;
  }

  @Override
  public double getRotation() {
    return rotation;
  }

}
