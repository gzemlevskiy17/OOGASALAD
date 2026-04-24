package oogasalad.view.core.api;

import javafx.scene.Node;
import oogasalad.view.core.internal.SpriteCache;

/**
 * View-side description of how to draw and animate one component type. Produced by a
 * {@link SpriteRegistry} and consumed by the rendering pipeline.
 *
 * <p>Implementations are stateless. Each call returns a fresh {@link Node}.
 *
 * @author Paulina Vvedenskaya
 */
public sealed interface Sprite permits SingleSprite, LayeredSprite {

  String USES_RENDERABLE_ROTATION_KEY = "oogasalad.view.core.api.Sprite.usesRenderableRotation";

  double width();

  double height();

  /**
   * Draws the sprite with its top-left at (0,0) — suitable for embedding in a layout container.
   */
  Node renderPreview(SpriteCache cache);

  /**
   * Draws the sprite centered around (0,0) — suitable for placement by the world renderer.
   */
  default Node render(SpriteCache cache) {
    Node node = renderPreview(cache);
    node.setManaged(false);
    node.setTranslateX(-width() / 2);
    node.setTranslateY(-height() / 2);
    node.getProperties().put(USES_RENDERABLE_ROTATION_KEY, usesRenderableRotation());
    return node;
  }

  /**
   * Whether the renderer should rotate the entire sprite node using the body rotation.
   */
  default boolean usesRenderableRotation() {
    return true;
  }

  /**
   * Optional per-frame update (e.g. spinning a tire). Called with the node returned by render.
   */
  default void update(Node node, double linearSpeed) {
  }
}
