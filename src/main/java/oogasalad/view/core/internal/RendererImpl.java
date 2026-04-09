package oogasalad.view.core.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.layout.Pane;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Renderer;

public class RendererImpl implements Renderer {

  private final Pane renderLayer;


  public RendererImpl(Pane renderLayer) throws IllegalArgumentException{
    if (renderLayer == null) {
      throw new IllegalArgumentException("renderLayer cannot be null");
    }

    this.renderLayer = renderLayer;
  }

  public Pane getRenderLayer() {
    return renderLayer;
  }

  @Override
  public void render(Camera camera, Collection<? extends Renderable> objects) {
    if (camera == null || objects == null) {
      return;
    }

    Set<Renderable> seenThisFrame = new HashSet<>();

    for (Renderable object : objects) {
      if (object == null) {
        continue;
      }

      renderObject(object, camera);
    }
  }

  private void renderObject(Renderable object, Camera camera) {
    // Simple rule for now
    if (camera.isVisible(object)) {

    }
  }

}
