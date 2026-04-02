package oogasalad.view.authoring.api;

import oogasalad.model.engine.api.GameObject;

/**
 * Metadata for an occupied authoring-grid region and its drawing bounds, linked to the
 * {@link GameObject} it represents.
 */
public class PlacedObject {

  private final int col;
  private final int row;
  private final double drawX;
  private final double drawY;
  private final double width;
  private final double height;
  private final GameObject gameObject;

  public PlacedObject(int col, int row, double drawX, double drawY,
      double width, double height, GameObject gameObject) {
    this.col = col;
    this.row = row;
    this.drawX = drawX;
    this.drawY = drawY;
    this.width = width;
    this.height = height;
    this.gameObject = gameObject;
  }

  public int getCol() {
    return col;
  }

  public int getRow() {
    return row;
  }

  public double getDrawX() {
    return drawX;
  }

  public double getDrawY() {
    return drawY;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  public GameObject getGameObject() {
    return gameObject;
  }
}
