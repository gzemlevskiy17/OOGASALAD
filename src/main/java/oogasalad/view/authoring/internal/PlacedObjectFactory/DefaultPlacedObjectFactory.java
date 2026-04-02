package oogasalad.view.authoring.internal.PlacedObjectFactory;

import oogasalad.model.engine.api.GameObject;
import oogasalad.view.authoring.api.PlacedObject;
import oogasalad.view.authoring.api.PlacedObjectFactory;

/**
 * Default factory that creates a block whose pixel dimensions are converted to cell spans.
 */
public class DefaultPlacedObjectFactory implements PlacedObjectFactory {

  private final double widthPx;
  private final double heightPx;
  private final GameObject gameObject;

  public DefaultPlacedObjectFactory(double widthPx, double heightPx, GameObject gameObject) {
    this.widthPx = widthPx;
    this.heightPx = heightPx;
    this.gameObject = gameObject;
  }

  @Override
  public PlacedObject create(int col, int row, double cellSize) {
    int spanCols = colSpan(cellSize);
    int spanRows = rowSpan(cellSize);
    return new PlacedObject(col, row,
        col * cellSize, row * cellSize,
        spanCols * cellSize, spanRows * cellSize,
        gameObject);
  }

  @Override
  public int colSpan(double cellSize) {
    return (int) Math.ceil(widthPx / cellSize);
  }

  @Override
  public int rowSpan(double cellSize) {
    return (int) Math.ceil(heightPx / cellSize);
  }
}
