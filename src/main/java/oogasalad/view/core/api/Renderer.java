package oogasalad.view.core.api;

import java.util.Collection;

/** Contract for drawing a list of objects. */
public interface Renderer {
  /**
   * Draws the provided objects from the perspective of the given camera.
   * @param camera The viewport determining visibility and offset.
   * @param objects The collection of objects to potentially draw.
   */
  void render(Camera camera, Collection<? extends Renderable> objects);
}