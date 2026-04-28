package oogasalad.view.authoring.api;

import javafx.scene.Node;

/** Contract for the visual grid where blocks and game elements are placed. */
public interface AuthoringCanvas {
  /** @return the visual root of the grid to be embedded in the editor UI. */
  Node getVisualRoot();

  /** Refreshes the visual state of the grid to reflect the current model. */
  void refreshGrid();

  /**
   * Swaps the currently selected placeable (called by the controller/palette).
   */
  void setSelectedPlaceable(AuthoringPlaceable placeable);

  /** @return true if the given grid cell is within the canvas bounds. */
  boolean isWithinBounds(int col, int row);

  /** @return the cell size in pixels. */
  double getCellSize();

  /**
   * Places a block at the given cell, spanning multiple cells based on the
   * current factory.
   */
  void placeBlock(int col, int row);

  /**
   * Shows a semi-transparent hover preview at the given grid cell using the
   * current factory's span.
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
   * Registers a callback invoked when the user clicks a placed object on the
   * canvas.
   */
  void setOnObjectSelected(java.util.function.Consumer<AuthoringPlaceable> handler);

  /**
   * Fired when the pointer enters a placed terrain object (for showing catalog-style metadata while
   * hovering).
   *
   * <p>Default is a no-op for test doubles.
   */
  default void setOnTerrainHoverEnter(
      java.util.function.Consumer<AuthoringPlaceable> handler) {
  }

  /**
   * Fired when the pointer leaves a placed terrain object (restore selection summary or
   * placeholder).
   *
   * <p>Default is a no-op for test doubles.
   */
  default void setOnTerrainHoverExit(Runnable handler) {
  }

  /**
   * Sets the visual zoom level (1.0 = 100%). Grid coordinates are unaffected.
   * Clamped to [0.25, 4.0].
   */
  void setZoom(double factor);

  /** @return the current zoom factor. */
  double getZoom();

  /**
   * Adds a transient overlay node (for example a spline-draft polygon ghost) on top of the grid
   * without touching the model. The node is removed on the next {@link #refreshGrid()} call, or
   * when {@link #clearOverlays()} is invoked explicitly.
   *
   * <p>Default implementation is a no-op so test doubles and alternate canvas implementations
   * that do not need draft overlays can opt out without extra boilerplate.
   */
  default void addOverlay(Node node) {}

  /**
   * Removes every overlay previously added via {@link #addOverlay(Node)}.
   *
   * <p>Default implementation is a no-op for the same reason as {@link #addOverlay(Node)}.
   */
  default void clearOverlays() {}

  /**
   * Sets an optional sky/background image behind the authoring grid.
   *
   * <p>Default implementation is a no-op for tests and alternate canvases.
   */
  default void setBackgroundImagePath(String imagePath) {}
}