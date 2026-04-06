package oogasalad.view.authoring.api;

/**
 * Provides the span and color of the object type currently selected in the palette.
 * The controller swaps in a different factory whenever the user selects a new object kind.
 */
public interface PlacedObjectFactory {

  /** @return number of columns this object spans, given the cell size. */
  int colSpan(double cellSize);

  /** @return number of rows this object spans, given the cell size. */
  int rowSpan(double cellSize);

  /** @return the color hex string for this object type. */
  String getColor();
}
