package oogasalad.view.authoring.internal;

import oogasalad.view.authoring.api.PlacedObjectFactory;

/**
 * Default factory that provides span, color, and terrain metadata for a particular object type.
 */
public class DefaultPlacedObjectFactory implements PlacedObjectFactory {

  private final double widthPx;
  private final double heightPx;
  private final String color;
  private final String sprite;
  private final boolean tiled;
  private final double friction;

  public DefaultPlacedObjectFactory(double widthPx, double heightPx, String color,
      String sprite, boolean tiled, double friction) {
    this.widthPx = widthPx;
    this.heightPx = heightPx;
    this.color = color;
    this.sprite = sprite;
    this.tiled = tiled;
    this.friction = friction;
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

  @Override
  public String getSprite() {
    return sprite;
  }

  @Override
  public boolean isTiled() {
    return tiled;
  }

  @Override
  public double getFriction() {
    return friction;
  }
}
