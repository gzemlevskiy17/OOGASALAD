package oogasalad.view.authoring.internal;

import oogasalad.view.authoring.api.PlacedObjectFactory;

/**
 * Default factory that provides span and color for a particular object type.
 */
public class DefaultPlacedObjectFactory implements PlacedObjectFactory {

  private final double widthPx;
  private final double heightPx;
  private final String color;

  public DefaultPlacedObjectFactory(double widthPx, double heightPx, String color) {
    this.widthPx = widthPx;
    this.heightPx = heightPx;
    this.color = color;
  }

  @Override
  public int colSpan(double cellSize) {
    return (int) Math.ceil(widthPx / cellSize);
  }

  @Override
  public int rowSpan(double cellSize) {
    return (int) Math.ceil(heightPx / cellSize);
  }

  @Override
  public String getColor() {
    return color;
  }
}