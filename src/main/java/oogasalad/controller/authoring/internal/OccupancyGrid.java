package oogasalad.controller.authoring.internal;

import java.util.Collection;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Immutable snapshot of which grid cells are occupied and by which object.
 *
 * <p>Built once from the current set of {@link AuthoringPlaceable} objects and the grid
 * dimensions. The internal array is never exposed — all access goes through read-only
 * query methods.
 */
public class OccupancyGrid {

  private final AuthoringPlaceable[][] grid;
  private final int cols;
  private final int rows;

  /**
   * Constructs an occupancy snapshot by scanning all placeables and marking the cells
   * they cover.
   *
   * <p>For each placeable, cells from {@code (int) left()} to {@code (int) ceil(right())}
   * (exclusive) and {@code (int) top()} to {@code (int) ceil(bottom())} (exclusive) are
   * marked. {@code Math.ceil} on the far edges ensures partial cell coverage counts as
   * occupied, preventing flood-fill leaks through boundary cracks.
   *
   * @param placeables objects currently in the world
   * @param cols       number of grid columns
   * @param rows       number of grid rows
   */
  public OccupancyGrid(Collection<? extends AuthoringPlaceable> placeables, int cols, int rows) {
    this.cols = cols;
    this.rows = rows;
    this.grid = new AuthoringPlaceable[cols][rows];

    for (AuthoringPlaceable p : placeables) {
      int colStart = Math.max(0, (int) p.left());
      int colEnd = Math.min(cols, (int) Math.ceil(p.right()));
      int rowStart = Math.max(0, (int) p.top());
      int rowEnd = Math.min(rows, (int) Math.ceil(p.bottom()));

      for (int c = colStart; c < colEnd; c++) {
        for (int r = rowStart; r < rowEnd; r++) {
          grid[c][r] = p;
        }
      }
    }
  }

  /** Returns {@code true} if the cell at (col, row) is within the grid bounds. */
  public boolean isInBounds(int col, int row) {
    return col >= 0 && col < cols && row >= 0 && row < rows;
  }

  /** Returns {@code true} if the cell at (col, row) is occupied by any placeable. */
  public boolean isOccupied(int col, int row) {
    return isInBounds(col, row) && grid[col][row] != null;
  }

  /**
   * Returns the placeable occupying the cell at (col, row), or {@code null} if the cell
   * is empty or out of bounds.
   */
  public AuthoringPlaceable getObjectAt(int col, int row) {
    if (!isInBounds(col, row)) {
      return null;
    }
    return grid[col][row];
  }

  public int getCols() {
    return cols;
  }

  public int getRows() {
    return rows;
  }
}
