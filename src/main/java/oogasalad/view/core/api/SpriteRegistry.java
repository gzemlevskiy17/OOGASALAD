package oogasalad.view.core.api;

/**
 * Resolves a {@link Renderable} to the {@link Sprite} that knows how to draw it. Uniform entry
 * point for the renderer — palette-driven components and instance-sized components go through the
 * same call.
 *
 * @author Paulina Vvedenskaya
 */
public interface SpriteRegistry {

  Sprite resolve(Renderable renderable);
}
