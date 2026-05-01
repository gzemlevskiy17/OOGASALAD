package oogasalad.view.core.api;

import javafx.scene.Node;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Describes how to build and optionally animate JavaFX Nodes for palette previews and RendererImpl-backed bodies.
 * Implementations should be effectively stateless; each render call may create a new Node graph.
 *
 * @author Paulina Vvedenskaya
 */
public sealed interface Sprite permits SingleSprite, LayeredSprite {

  /** Node property key: when false, RendererImpl skips applying Renderable rotation to the outer node. */
  String USES_RENDERABLE_ROTATION_KEY = "oogasalad.view.core.api.Sprite.usesRenderableRotation";

  /** Logical width in sprite units. */
  double width();

  /** Logical height in sprite units. */
  double height();

  /**
   * Renders with top-left at the origin, suitable for palette thumbnails and layout embedding.
   */
  Node renderPreview(SpriteCache cache);

  /**
   * Renders centered on the origin, unmanaged, with rotation hint properties for the renderer.
   */
  default Node render(SpriteCache cache) {
    Node node = renderPreview(cache);
    node.setManaged(false);
    node.setTranslateX(-width() / 2);
    node.setTranslateY(-height() / 2);
    node.getProperties().put(USES_RENDERABLE_ROTATION_KEY, usesRenderableRotation());
    return node;
  }

  /** When true, RendererImpl applies body rotation in degrees to the node. */
  default boolean usesRenderableRotation() {
    return true;
  }

  /**
   * Optional per-frame cosmetic update such as spinning a wheel layer using signed rolling speed input.
   */
  default void update(Node node, double animationSpeed) {
  }
}
