package oogasalad.view.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.api.Sprite;

/**
 * Default renderer: keeps one JavaFX node per renderable instance, reuses it across frames,
 * sets layout from world position minus camera position, applies rotation when the node allows
 * it (see Sprite.USES_RENDERABLE_ROTATION_KEY on the node’s properties), runs an optional
 * per-frame callback to sync visuals, and removes nodes whose renderables were not passed in
 * this frame. Uses identity, not equals, to match renderables to nodes.
 *
 * @author Iurii Beliaev
 */

public class RendererImpl implements Renderer {

  private static final double DEFAULT_MARKER_RADIUS = 4.0;
  private static final BiConsumer<Renderable, Node> NO_OP_FRAME_UPDATER = (r, n) -> {
  };
  private static final Function<Renderable, Node> DEFAULT_NODE_FACTORY =
      r -> new Circle(DEFAULT_MARKER_RADIUS);

  private final Pane renderLayer;
  private final Function<? super Renderable, ? extends Node> nodeFactory;
  private final BiConsumer<? super Renderable, ? super Node> frameUpdater;
  private final Map<Renderable, Node> nodesByRenderable = new IdentityHashMap<>();

  public RendererImpl(Pane renderLayer) {
    this(renderLayer, DEFAULT_NODE_FACTORY, NO_OP_FRAME_UPDATER);
  }

  public RendererImpl(Pane renderLayer,
      Function<? super Renderable, ? extends Node> nodeFactory,
      BiConsumer<? super Renderable, ? super Node> frameUpdater) {
    if (renderLayer == null || nodeFactory == null || frameUpdater == null) {
      throw new IllegalArgumentException("RendererImpl requires non-null arguments");
    }
    this.renderLayer = renderLayer;
    this.nodeFactory = nodeFactory;
    this.frameUpdater = frameUpdater;
  }

  @Override
  public void render(Camera camera, Collection<? extends Renderable> objects) {
    if (camera == null || objects == null) {
      return;
    }
    Set<Renderable> seenThisFrame = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Renderable object : objects) {
      if (object == null) {
        continue;
      }
      if (!camera.isVisible(object)) {
        continue;
      }
      seenThisFrame.add(object);
      renderObject(object, camera);
    }
    removeNodesNotSeenThisFrame(seenThisFrame);
  }

  private void renderObject(Renderable object, Camera camera) {
    Node node = nodesByRenderable.computeIfAbsent(object, this::createAndAttachNode);
    node.setManaged(false);
    node.setLayoutX(object.x() - camera.getX());
    node.setLayoutY(object.y() - camera.getY());
    node.setRotate(usesRenderableRotation(node) ? Math.toDegrees(object.rotation()) : 0.0);
    node.setVisible(true);
    frameUpdater.accept(object, node);
  }

  private boolean usesRenderableRotation(Node node) {
    return !Boolean.FALSE.equals(node.getProperties().get(Sprite.USES_RENDERABLE_ROTATION_KEY));
  }

  private Node createAndAttachNode(Renderable object) {
    Node node = nodeFactory.apply(object);
    if (node == null) {
      throw new IllegalStateException("nodeFactory returned null for renderable: " + object);
    }
    renderLayer.getChildren().add(node);
    return node;
  }

  private void removeNodesNotSeenThisFrame(Set<Renderable> seenThisFrame) {
    for (Renderable tracked : new ArrayList<>(nodesByRenderable.keySet())) {
      if (!seenThisFrame.contains(tracked)) {
        Node node = nodesByRenderable.remove(tracked);
        if (node != null) {
          renderLayer.getChildren().remove(node);
        }
      }
    }
  }
}
