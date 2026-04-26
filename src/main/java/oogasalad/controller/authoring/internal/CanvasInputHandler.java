package oogasalad.controller.authoring.internal;

import javafx.scene.Node;

/**
 * One gesture-stream handler for the authoring canvas. Implementations register their own listeners
 * on the canvas root and own any per-gesture state.
 */
public interface CanvasInputHandler {

  void register(Node root);
}
