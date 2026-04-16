package oogasalad.view.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Renderer;

public class RendererImpl implements Renderer {

  private static final double DEFAULT_MARKER_RADIUS = 4.0;

  private final Pane renderLayer;
  private final Function<? super Renderable, ? extends Node> nodeFactory;
  private final Map<Renderable, Node> nodesByRenderable = new IdentityHashMap<>(); // Create a node once and reuse it

  // If the user doesn't provide a way to build nodes, default to drawing circles
  public RendererImpl(Pane renderLayer) {
    this(renderLayer, RendererImpl::createDefaultNode);
  }

  // Dependency Injection + Factory pattern
  public RendererImpl(Pane renderLayer, Function<? super Renderable, ? extends Node> nodeFactory) {
    if (renderLayer == null) {
      throw new IllegalArgumentException("renderLayer cannot be null");
    }
    if (nodeFactory == null) {
      throw new IllegalArgumentException("nodeFactory cannot be null");
    }

    this.renderLayer = renderLayer;
    this.nodeFactory = nodeFactory;
  }

  public Pane getRenderLayer() {
    return renderLayer;
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

//      if (!camera.isVisible(object)) {
//        removeNode(object);
//        continue;
//      }

      seenThisFrame.add(object);
      renderObject(object, camera);
    }

    removeNodesNotSeenThisFrame(seenThisFrame);
  }

  private void renderObject(Renderable object, Camera camera) {
    Node node = nodesByRenderable.computeIfAbsent(object, this::createAndAttachNode);

    node.setManaged(false);
    node.setLayoutX(object.getX() - camera.getX());
    node.setLayoutY(object.getY() - camera.getY());
    node.setRotate(object.getRotation());
    node.setVisible(true);
  }

  private Node createAndAttachNode(Renderable object) {
    Node node = nodeFactory.apply(object);
    if (node == null) {
      throw new IllegalStateException("nodeFactory returned null for renderable: " + object);
    }

    if (!renderLayer.getChildren().contains(node)) {
      renderLayer.getChildren().add(node);
    }
    return node;
  }

  private void removeNode(Renderable object) {
    Node node = nodesByRenderable.remove(object);
    if (node != null) {
      renderLayer.getChildren().remove(node);
    }
  }

  private void removeNodesNotSeenThisFrame(Set<Renderable> seenThisFrame) {
    for (Renderable tracked : new ArrayList<>(nodesByRenderable.keySet())) {
      if (!seenThisFrame.contains(tracked)) {
        removeNode(tracked);
      }
    }
  }

  private static Node createDefaultNode(Renderable ignored) {
    return new Circle(DEFAULT_MARKER_RADIUS);
  }
}