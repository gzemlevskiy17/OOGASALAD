package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Routes mouse input to the spline draft controller while the SPLINE tool is active:
 * primary click adds a knot, secondary click commits the draft, mouse-move updates the
 * draft cursor. Listeners no-op when a different tool is selected so this handler can
 * coexist with others on the same node.
 *
 * @author Paulina Vvedenskaya
 */
public class SplineInputHandler implements CanvasInputHandler {

  private final AuthoringCanvas canvas;
  private final ToolSelector toolSelector;
  private final SplineDraftController splineDraft;

  /**
   * @param canvas       canvas providing cell size and overlay refresh
   * @param toolSelector source of truth for the currently-active tool
   * @param splineDraft  knot draft this handler feeds clicks into
   */
  public SplineInputHandler(AuthoringCanvas canvas, ToolSelector toolSelector,
      SplineDraftController splineDraft) {
    this.canvas = Objects.requireNonNull(canvas);
    this.toolSelector = Objects.requireNonNull(toolSelector);
    this.splineDraft = Objects.requireNonNull(splineDraft);
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handleMousePressed(event));
    root.addEventHandler(MouseEvent.MOUSE_MOVED, event -> handleMouseMoved(event));
    root.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> handleMouseMoved(event));
  }

  private void handleMousePressed(MouseEvent event) {
    if (!isSplineMode()) {
      return;
    }
    if (event.getButton() == MouseButton.PRIMARY) {
      splineDraft.addKnot(colOf(event), rowOf(event));
    } else if (event.getButton() == MouseButton.SECONDARY) {
      splineDraft.commit();
      canvas.refreshGrid();
    }
  }

  private void handleMouseMoved(MouseEvent event) {
    if (!isSplineMode()) {
      return;
    }
    canvas.clearHoverPreview();
    splineDraft.moveCursor(colOf(event), rowOf(event));
  }

  private boolean isSplineMode() {
    return toolSelector.getActiveTool() == ToolType.SPLINE;
  }

  private double colOf(MouseEvent event) {
    return event.getX() / canvas.getCellSize();
  }

  private double rowOf(MouseEvent event) {
    return event.getY() / canvas.getCellSize();
  }
}
