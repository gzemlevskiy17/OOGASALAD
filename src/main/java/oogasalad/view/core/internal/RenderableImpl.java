package oogasalad.view.core.internal;

import oogasalad.view.core.api.Renderable;

/**
 * Simple immutable Renderable for tests and camera math without live GameObject instances.
 *
 * @param x        world x-coordinate
 * @param y        world y-coordinate
 * @param rotation rotation in radians
 * @author Iurii Beliaev
 */
public record RenderableImpl(double x, double y, double rotation) implements Renderable {}
