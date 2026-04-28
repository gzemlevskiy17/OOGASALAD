package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Places the currently selected palette item when the user clicks an empty canvas cell.
 *
 * <p>The handler ignores spline mode and clicks that land on existing placed objects so
 * object selection can coexist with click-to-place behavior.
 */
public class ClickPlaceInputHandler implements CanvasInputHandler {

  private final AuthoringCanvas canvas;
  private final ToolSelector toolSelector;
  private final PaletteDropInputHandler.DropPlacer placer;

  /**
   * @param canvas canvas that exposes grid sizing and bounds checks
   * @param toolSelector source of truth for the active authoring tool
   * @param placer callback to invoke on valid click placement
   */
  public ClickPlaceInputHandler(AuthoringCanvas canvas, ToolSelector toolSelector,
      PaletteDropInputHandler.DropPlacer placer) {
    this.canvas = Objects.requireNonNull(canvas);
    this.toolSelector = Objects.requireNonNull(toolSelector);
    this.placer = Objects.requireNonNull(placer);
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handleMousePressed(root, event));
  }

  private void handleMousePressed(Node root, MouseEvent event) {
    if (toolSelector.getActiveTool() == ToolType.SPLINE || event.getButton() != MouseButton.PRIMARY) {
      return;
    }
    if (!isCanvasSurfaceClick(root, event.getTarget())) {
      return;
    }
    int col = (int) Math.floor(event.getX() / canvas.getCellSize());
    int row = (int) Math.floor(event.getY() / canvas.getCellSize());
    if (!canvas.isWithinBounds(col, row)) {
      return;
    }
    placer.placeAt(col, row);
  }

  private boolean isCanvasSurfaceClick(Node root, Object target) {
    return target == root || target instanceof Line;
  }
}
