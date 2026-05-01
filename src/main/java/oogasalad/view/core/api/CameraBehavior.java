package oogasalad.view.core.api;

/**
 * Strategy that updates Camera position or other viewport state once per simulation or animation tick.
 *
 * @author Iurii Beliaev
 */
public interface CameraBehavior {

  /** Applies this behavior by mutating the given camera for the current frame. */
  void update(Camera camera);
}
