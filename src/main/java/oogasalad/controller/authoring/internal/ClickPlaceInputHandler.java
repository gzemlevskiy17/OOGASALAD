package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.controller.authoring.internal.PaletteDropInputHandler.DropPlacer;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Places the currently-selected palette item on primary-button click. Inactive during spline
 * drafting so clicks go to the spline handler instead.
 *
 * @author Paulina Vvedenskaya
 */
public class ClickPlaceInputHandler implements CanvasInputHandler {

  private final AuthoringCanvas canvas;
  private final ToolSelector toolSelector;
  private final DropPlacer placer;

  /**
   * @param canvas       canvas providing grid coordinates and bounds checking
   * @param toolSelector source of truth for the currently-active tool
   * @param placer       callback to fire when a valid cell is clicked
   */
  public ClickPlaceInputHandler(AuthoringCanvas canvas, ToolSelector toolSelector,
      DropPlacer placer) {
    this.canvas = Objects.requireNonNull(canvas);
    this.toolSelector = Objects.requireNonNull(toolSelector);
    this.placer = Objects.requireNonNull(placer);
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleClick(event));
  }

  private void handleClick(MouseEvent event) {
    if (event.getButton() != MouseButton.PRIMARY) {
      return;
    }
    if (toolSelector.getActiveTool() == ToolType.SPLINE) {
      return;
    }
    int col = colOf(event);
    int row = rowOf(event);
    if (!canvas.isWithinBounds(col, row)) {
      return;
    }
    placer.placeAt(col, row);
  }

  private int colOf(MouseEvent event) {
    return (int) Math.floor(event.getX() / canvas.getCellSize());
  }

  private int rowOf(MouseEvent event) {
    return (int) Math.floor(event.getY() / canvas.getCellSize());
  }
}
