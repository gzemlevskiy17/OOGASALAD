package oogasalad.view.core.internal;

import oogasalad.view.core.api.Renderable;

/**
 * Immutable value implementation of {@link Renderable}.
 *
 * @author Iurii Beliaev
 */
public record RenderableImpl(double x, double y, double rotation) implements Renderable {

  /**
   * Creates a renderable snapshot.
   *
   * @param x        world x-coordinate
   * @param y        world y-coordinate
   * @param rotation rotation in radians
   */
  public RenderableImpl {
  }

}
