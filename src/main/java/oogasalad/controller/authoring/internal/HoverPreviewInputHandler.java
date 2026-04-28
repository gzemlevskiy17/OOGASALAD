package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Shows a hover preview at the grid cell under the cursor while the user moves the mouse
 * over the canvas. Stays out of the way during spline drafting (which has its own cursor).
 *
 * @author Paulina Vvedenskaya
 */
public class HoverPreviewInputHandler implements CanvasInputHandler {

  private final AuthoringCanvas canvas;
  private final ToolSelector toolSelector;

  /**
   * @param canvas       canvas to drive the preview overlay on
   * @param toolSelector source of truth for the currently-active tool
   */
  public HoverPreviewInputHandler(AuthoringCanvas canvas, ToolSelector toolSelector) {
    this.canvas = Objects.requireNonNull(canvas);
    this.toolSelector = Objects.requireNonNull(toolSelector);
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.addEventHandler(MouseEvent.MOUSE_MOVED, event -> handleMouseMoved(event));
    root.addEventHandler(MouseEvent.MOUSE_EXITED, event -> handleMouseExited());
  }

  private void handleMouseMoved(MouseEvent event) {
    if (isSplineMode()) {
      return;
    }
    canvas.showHoverPreview(colOf(event), rowOf(event));
  }

  private void handleMouseExited() {
    canvas.clearHoverPreview();
  }

  private boolean isSplineMode() {
    return toolSelector.getActiveTool() == ToolType.SPLINE;
  }

  private int colOf(MouseEvent event) {
    return (int) Math.floor(event.getX() / canvas.getCellSize());
  }

  private int rowOf(MouseEvent event) {
    return (int) Math.floor(event.getY() / canvas.getCellSize());
  }
}
