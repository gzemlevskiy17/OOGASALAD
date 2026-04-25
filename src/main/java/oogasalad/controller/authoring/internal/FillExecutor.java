package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import oogasalad.controller.authoring.api.AuthoringPlacementException;

/**
 * Computes the set of cells reachable from a starting point via flood fill
 * on unoccupied cells.
 *
 * <p>Uses BFS with 4-connectivity (up/down/left/right). A configurable limit
 * prevents unbounded fills when the clicked region is not fully enclosed.
 */
public class FillExecutor {

  /** Maximum number of cells a single fill operation may cover by default. */
  public static final int DEFAULT_MAX_FILL = 5000;

  /** Outcome of an enclosed-region computation. */
  public enum FillStatus {
    FILLED,
    NOT_ENCLOSED,
    TOO_LARGE
  }

  /** Result of {@link #computeEnclosedRegion}. */
  public record FillResult(List<int[]> cells, FillStatus status) {
  }

  private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private final int maxFill;

  public FillExecutor() {
    this(DEFAULT_MAX_FILL);
  }

  public FillExecutor(int maxFill) {
    this.maxFill = maxFill;
  }

  /**
   * Computes all cells reachable from (startCol, startRow) via 4-connected
   * BFS on unoccupied cells, restricted to rows >= minRow.
   *
   * @param grid      immutable occupancy snapshot
   * @param startCol  column of the clicked cell
   * @param startRow  row of the clicked cell
   * @param minRow    rows below this index are excluded from the fill
   * @return list of [col, row] pairs to fill; empty if start is occupied or out of bounds
   * @throws AuthoringPlacementException if the fill region exceeds the configured maximum
   */
  public List<int[]> computeFillRegion(OccupancyGrid grid, int startCol, int startRow, int minRow) {
    if (!grid.isInBounds(startCol, startRow) || grid.isOccupied(startCol, startRow)
        || startRow < minRow) {
      return List.of();
    }

    boolean[][] visited = new boolean[grid.getCols()][grid.getRows()];
    Queue<int[]> queue = new LinkedList<>();
    List<int[]> result = new ArrayList<>();

    visited[startCol][startRow] = true;
    queue.add(new int[]{startCol, startRow});

    while (!queue.isEmpty()) {
      int[] cell = queue.poll();
      result.add(cell);

      if (result.size() > maxFill) {
        throw new AuthoringPlacementException(
            "Fill area too large (exceeded " + maxFill
                + " cells). The region may not be fully enclosed.");
      }

      for (int[] dir : DIRECTIONS) {
        int nc = cell[0] + dir[0];
        int nr = cell[1] + dir[1];
        if (grid.isInBounds(nc, nr) && nr >= minRow && !visited[nc][nr] && !grid.isOccupied(nc, nr)) {
          visited[nc][nr] = true;
          queue.add(new int[]{nc, nr});
        }
      }
    }

    return result;
  }

  /** Overload with no row restriction — fills the entire reachable region. */
  public List<int[]> computeFillRegion(OccupancyGrid grid, int startCol, int startRow) {
    return computeFillRegion(grid, startCol, startRow, 0);
  }

  /**
   * Computes an enclosed region reachable from (startCol, startRow). Returns the cells
   * only if the region is fully enclosed (BFS never touches a grid edge).
   *
   * @param grid      immutable occupancy snapshot
   * @param startCol  column of the start cell
   * @param startRow  row of the start cell
   * @return a {@link FillResult} with the cells and the outcome status
   */
  public FillResult computeEnclosedRegion(OccupancyGrid grid, int startCol, int startRow) {
    if (!grid.isInBounds(startCol, startRow) || grid.isOccupied(startCol, startRow)) {
      return new FillResult(List.of(), FillStatus.NOT_ENCLOSED);
    }

    boolean[][] visited = new boolean[grid.getCols()][grid.getRows()];
    Queue<int[]> queue = new LinkedList<>();
    List<int[]> result = new ArrayList<>();

    visited[startCol][startRow] = true;
    queue.add(new int[]{startCol, startRow});

    while (!queue.isEmpty()) {
      int[] cell = queue.poll();
      result.add(cell);

      if (result.size() > maxFill) {
        return new FillResult(List.of(), FillStatus.TOO_LARGE);
      }

      for (int[] dir : DIRECTIONS) {
        int nc = cell[0] + dir[0];
        int nr = cell[1] + dir[1];
        if (grid.isInBounds(nc, nr) && !visited[nc][nr] && !grid.isOccupied(nc, nr)) {
          visited[nc][nr] = true;
          queue.add(new int[]{nc, nr});
        }
      }
    }

    return new FillResult(result, FillStatus.FILLED);
  }
}
