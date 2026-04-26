package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Handles drag-and-drop placement from the palette onto the canvas. Shows a hover preview
 * while a drag is over the canvas, and notifies a placer when a valid drop occurs.
 */
public class PaletteDropInputHandler implements CanvasInputHandler {

  public interface DropPlacer {
    void placeAt(int col, int row);
  }

  private final AuthoringCanvas canvas;
  private final DropPlacer placer;

  public PaletteDropInputHandler(AuthoringCanvas canvas, DropPlacer placer) {
    this.canvas = Objects.requireNonNull(canvas);
    this.placer = Objects.requireNonNull(placer);
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.setOnDragOver(event -> handleDragOver(root, event));
    root.setOnDragExited(event -> handleDragExited(event));
    root.setOnDragDropped(event -> handleDragDropped(event));
  }

  private void handleDragOver(Node root, DragEvent event) {
    if (isPaletteDrag(root, event)) {
      event.acceptTransferModes(TransferMode.COPY);
      canvas.showHoverPreview(colOf(event), rowOf(event));
    }
    event.consume();
  }

  private void handleDragExited(DragEvent event) {
    canvas.clearHoverPreview();
    event.consume();
  }

  private void handleDragDropped(DragEvent event) {
    boolean placed = tryPlaceAt(event);
    event.setDropCompleted(placed);
    event.consume();
  }

  private boolean tryPlaceAt(DragEvent event) {
    if (!event.getDragboard().hasString()) {
      return false;
    }
    int col = colOf(event);
    int row = rowOf(event);
    if (!canvas.isWithinBounds(col, row)) {
      return false;
    }
    placer.placeAt(col, row);
    return true;
  }

  private boolean isPaletteDrag(Node root, DragEvent event) {
    return event.getGestureSource() != root && event.getDragboard().hasString();
  }

  private int colOf(DragEvent event) {
    return (int) Math.floor(event.getX() / canvas.getCellSize());
  }

  private int rowOf(DragEvent event) {
    return (int) Math.floor(event.getY() / canvas.getCellSize());
  }
}
