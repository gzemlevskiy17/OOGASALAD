package oogasalad.view.help.internal;

import javafx.scene.Node;

/**
 * Adds draggable popup behavior to a target node using a designated drag handle.
 * @author Emilio Garcia
 */
public class PopupDragSupport {

  private double dragAnchorSceneX;
  private double dragAnchorSceneY;
  private double dragAnchorTranslateX;
  private double dragAnchorTranslateY;

  /**
   * Wires dragging behavior so the target node follows drag events from the handle node.
   *
   * @param dragHandle node that receives press/drag events
   * @param target node whose translation is updated
   */
  public void enableDragging(Node dragHandle, Node target) {
    dragHandle.setOnMousePressed(event -> {
      dragAnchorSceneX = event.getSceneX();
      dragAnchorSceneY = event.getSceneY();
      dragAnchorTranslateX = target.getTranslateX();
      dragAnchorTranslateY = target.getTranslateY();
    });
    dragHandle.setOnMouseDragged(event -> {
      target.setTranslateX(dragAnchorTranslateX + event.getSceneX() - dragAnchorSceneX);
      target.setTranslateY(dragAnchorTranslateY + event.getSceneY() - dragAnchorSceneY);
    });
  }
}
