package oogasalad.view.core.api;

/** Logic for how the camera should move. */
public interface CameraBehavior {
  /** * Updates the camera's position/zoom based on its internal logic.
   * @param camera The camera to be moved.
   */
  void update(Camera camera);
}