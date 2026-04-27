package oogasalad.view.authoring.internal;

import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Terrain placeable definition used by the palette and canvas preview.
 */
public class TerrainPlaceable implements AuthoringPlaceable {

  private final String catalogName;
  private final double widthPx;
  private final double heightPx;
  private final String color;
  private final String sprite;
  private final boolean tiled;
  private final double friction;
  private final double restitution;

  public TerrainPlaceable(double widthPx, double heightPx, String color,
      String sprite, boolean tiled, double friction, double restitution) {
    this(null, widthPx, heightPx, color, sprite, tiled, friction, restitution);
  }

  public TerrainPlaceable(String catalogName, double widthPx, double heightPx, String color,
      String sprite, boolean tiled, double friction, double restitution) {
    this.catalogName = catalogName;
    this.widthPx = widthPx;
    this.heightPx = heightPx;
    this.color = color;
    this.sprite = sprite;
    this.tiled = tiled;
    this.friction = friction;
    this.restitution = restitution;
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
  public double x() {
    return 0;
  }

  @Override
  public double y() {
    return 0;
  }

  @Override
  public double rotation() {
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
  public double getRestitution() {
    return restitution;
  }

  @Override
  public String getCatalogName() {
    return catalogName;
  }

  @Override
  public AuthoringPlaceable createAt(int col, int row, double cellSize) {
    return new TerrainBlock(
        col,
        row,
        colSpan(cellSize),
        rowSpan(cellSize),
        sprite,
        tiled,
        friction,
        color,
        restitution,
        catalogName);
  }
}
