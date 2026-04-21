package oogasalad.view.core.api;

/**
 * Marker interface for renderables that should be drawn using a {@link Sprite} resolved from a
 * {@link SpriteRegistry}. The component only exposes its type; the view owns all sprite paths and
 * dimensions.
 */
public interface SpriteRenderable extends Renderable {

  String getComponentType();
}
