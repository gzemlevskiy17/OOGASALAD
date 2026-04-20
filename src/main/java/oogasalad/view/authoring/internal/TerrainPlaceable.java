package oogasalad.view.authoring.internal;

import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Terrain placeable definition used by the palette and canvas preview.
 */
public class TerrainPlaceable implements AuthoringPlaceable {

  private final double widthPx;
  private final double heightPx;
  private final String color;
  private final String sprite;
  private final boolean tiled;
  private final double friction;

  public TerrainPlaceable(double widthPx, double heightPx, String color,
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
  public double getX() {
    return 0;
  }

  @Override
  public double getY() {
    return 0;
  }

  @Override
  public double getWidth() {
    return widthPx;
  }

  @Override
  public double getHeight() {
    return heightPx;
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

  @Override
  public AuthoringPlaceable createAt(int col, int row, double cellSize) {
    return new TerrainBlock(
        col,
        row,
        colSpan(cellSize),
        rowSpan(cellSize),
        tiled,
        friction,
        color);
  }
}
