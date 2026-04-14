package oogasalad.view.authoring.api;

import java.util.function.Consumer;
import javafx.scene.Node;

/** Contract for the visual grid where blocks and game elements are placed. */
public interface AuthoringCanvas {
  /** @return the visual root of the grid to be embedded in the editor UI. */
  Node getVisualRoot();

  /** Refreshes the visual state of the grid to reflect the current model. */
  void refreshGrid();

  /** Swaps the currently selected placeable (called by the controller/palette). */
  void setSelectedPlaceable(AuthoringPlaceable placeable);

  /** @return true if the given grid cell is within the canvas bounds. */
  boolean isWithinBounds(int col, int row);

  /** @return the cell size in pixels. */
  double getCellSize();

  /**
   * Places a block at the given cell, spanning multiple cells based on the current factory.
   */
  void placeBlock(int col, int row);

  /**
   * Shows a semi-transparent hover preview at the given grid cell using the current factory's span.
   */
  void showHoverPreview(int col, int row);

  /**
   * Removes any currently displayed hover preview.
   */
  void clearHoverPreview();

  /** @return the number of columns in the grid. */
  int getGridCols();

  /** @return the number of rows in the grid. */
  int getGridRows();

  /**
   * Registers a callback invoked when the user clicks a placed object on the canvas.
   */
  void setOnObjectSelected(java.util.function.Consumer<AuthoringPlaceable> handler);
}
