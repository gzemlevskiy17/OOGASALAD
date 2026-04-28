package oogasalad.view.core.api;

import java.util.Collection;

/**
 * Draws the game world for one frame: takes the current camera and the set of things that
 * should appear, and updates the view accordingly. Implementations decide how objects become
 * nodes, how the camera offset is applied, and how to drop things that are no longer in the list.
 *
 * @author Iurii Beliaev
 */
public interface Renderer {
  /**
   * Renders the given objects as seen from the camera. The camera supplies position (and
   * typically bounds) so the implementation can place or cull objects. Null camera or null
   * collection should be handled safely by the implementation (often by doing nothing).
   *
   * @param camera viewport / eye for this frame
   * @param objects things to draw this frame; may be empty
   */
  void render(Camera camera, Collection<? extends Renderable> objects);
}