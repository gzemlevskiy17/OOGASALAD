package oogasalad.view.core.api;

/**
 * Strategy interface that updates camera state each frame.
 *
 * @author Iurii Beliaev
 */
public interface CameraBehavior {
  /**
   * Updates the supplied camera according to implementation-specific behavior.
   *
   * @param camera camera instance to mutate
   */
  void update(Camera camera);
}