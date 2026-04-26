package oogasalad.controller.authoring.internal;

import javafx.scene.Node;

/**
 * One gesture-stream handler for the authoring canvas. Implementations register their own
 * listeners on the canvas root and own any per-gesture state, replacing the older pattern of
 * stuffing every mouse handler into one giant {@code initializeCanvasHandlers} method.
 *
 * @author Paulina Vvedenskaya
 */
public interface CanvasInputHandler {

  /**
   * Attaches this handler's listeners to the given canvas root node.
   *
   * @param root the visual root of the authoring canvas
   */
  void register(Node root);
}
