package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Lightweight authoring-time representation of a catalog static object such as a coin, bomb,
 * heart, or finish trigger. Gameplay loading resolves the same type id into its runtime effect.
 */
public class AuthoringStaticComponent extends PassThroughComponent
    implements AuthoringPlaceable, TypedComponent {

  private final String typeId;
  private final double width;
  private final double height;
  private final String color;
  private final String sprite;

  public AuthoringStaticComponent(double x, double y, double width, double height,
      String typeId, String color, String sprite) {
    super(x, y, 0, ConvexPolygon.axisAlignedBox(x, y, width, height), "");
    this.typeId = typeId;
    this.width = width;
    this.height = height;
    this.color = color;
    this.sprite = sprite;
  }

  @Override
  public String getComponentType() {
    return typeId;
  }

  @Override
  public String getDisplayName() {
    return typeId;
  }

  @Override
  public double getWidth() {
    return width;
  }

  @Override
  public double getHeight() {
    return height;
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
    return false;
  }

  @Override
  public double getFriction() {
    return 0;
  }

  @Override
  public double getRestitution() {
    return 0;
  }

  @Override
  public AuthoringPlaceable createAt(int col, int row, double cellSize) {
    return new AuthoringStaticComponent(col, row, colSpan(cellSize), rowSpan(cellSize), typeId,
        color, sprite);
  }
}
