package oogasalad.view.authoring.api;

import oogasalad.model.engine.api.GameObject;

/**
 * Represents an object definition currently selected in the authoring palette.
 * Implementations provide preview metadata and know how to instantiate their
 * backend representation when placed on the canvas.
 */
public interface Placeable {

  /** @return number of columns this object spans, given the cell size. */
  int colSpan(double cellSize);

  /** @return number of rows this object spans, given the cell size. */
  int rowSpan(double cellSize);

  /** @return the color hex string for this object type. */
  String getColor();

  /** @return the texture/sprite path for this terrain type. */
  String getSprite();

  /** @return true if the texture should tile, false to stretch. */
  boolean isTiled();

  /** @return the friction coefficient for this terrain type. */
  double getFriction();

  /**
   * Creates the backend object anchored at the given grid cell.
   *
   * @param col selected grid column
   * @param row selected grid row
   * @param cellSize size of one grid cell in pixels
   * @return the model object to add to the World
   */
  GameObject createAt(int col, int row, double cellSize);

}
