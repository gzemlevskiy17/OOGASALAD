package oogasalad.view.core.internal;

import oogasalad.view.core.api.Renderable;

/**
 * Immutable value implementation of {@link Renderable}.
 *
 * @param x        world x-coordinate
 * @param y        world y-coordinate
 * @param rotation rotation in radians
 */
public record RenderableImpl(double x, double y, double rotation) implements Renderable {}