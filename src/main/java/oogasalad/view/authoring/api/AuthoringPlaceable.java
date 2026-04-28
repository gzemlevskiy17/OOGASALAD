package oogasalad.view.authoring.api;

import oogasalad.view.core.api.Renderable;

/**
 * Represents a placeable object in the authoring environment.
 *
 * <p>This contract unifies palette selection metadata, rectangular bounds, and
 * backend object creation so the authoring controller and canvas can work with
 * one consistent abstraction.
 * @author Emilio Garcia
 */
public interface AuthoringPlaceable extends Renderable {

  double getWidth();

  double getHeight();

  default int colSpan(double cellSize) {
    return (int) Math.ceil(getWidth() / cellSize);
  }

  default int rowSpan(double cellSize) {
    return (int) Math.ceil(getHeight() / cellSize);
  }

  default String getDisplayName() {
    return getClass().getSimpleName();
  }

  default double left() {
    return x();
  }

  default double top() {
    return y();
  }

  default double right() {
    return left() + getWidth();
  }

  default double bottom() {
    return top() + getHeight();
  }

  String getColor();

  String getSprite();

  boolean isTiled();

  double getFriction();

  double getRestitution();

  /**
   * Display name from the terrain catalog (palette key), e.g. {@code Earth}, when known.
   *
   * @return the catalog id, or {@code null} for legacy/loaded content without that metadata
   */
  default String getCatalogName() {return null;}

  /**
   * Creates a placed backend object anchored at the given grid cell.
   *
   * @param col selected grid column
   * @param row selected grid row
   * @param cellSize size of one grid cell in pixels
   * @return the backend object to add to the authoring world
   */
  default AuthoringPlaceable createAt(int col, int row, double cellSize) {
    throw new UnsupportedOperationException("Placed authoring object cannot create another instance");
  }
}
