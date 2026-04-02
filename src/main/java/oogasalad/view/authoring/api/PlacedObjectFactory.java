package oogasalad.view.authoring.api;

/**
 * Factory that creates {@link PlacedObject} instances for a particular object type.
 * The controller swaps in a different factory whenever the user selects a new object kind.
 */
public interface PlacedObjectFactory {

  /**
   * Creates a placed object anchored at the given grid cell.
   *
   * @param col      anchor column
   * @param row      anchor row
   * @param cellSize pixel size of one grid cell
   * @return a new PlacedObject with correct draw bounds
   */
  PlacedObject create(int col, int row, double cellSize);

  /** @return number of columns this object spans, given the cell size. */
  int colSpan(double cellSize);

  /** @return number of rows this object spans, given the cell size. */
  int rowSpan(double cellSize);
}
