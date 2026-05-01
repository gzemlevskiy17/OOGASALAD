package oogasalad.view.core.api;

/**
 * Resolves a {@link Renderable} to the {@link Sprite} that knows how to draw it. Uniform entry
 * point for the renderer — palette-driven components and instance-sized components go through the
 * same call.
 *
 * @author Paulina Vvedenskaya
 * @author Iurii Beliaev
 */
public interface SpriteRegistry {

  /** Maps a gameplay renderable to its drawable Sprite representation. */
  Sprite resolve(Renderable renderable);
}
