package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.view.core.api.SpriteRenderable;

/**
 * Concrete dynamic component tagged as pigs.
 */
public class PigComponent extends DynamicComponent implements SpriteRenderable {

  private static final double DEFAULT_SIZE = 40.0;
  private final String componentType;

  public PigComponent(double x, double y, double rotation, double mass, double friction) {this(x, y, rotation, mass, friction, "Pig");}
  public PigComponent(double x, double y, double rotation, double mass, double friction,
      String componentType) {
    super(x, y, rotation, mass, friction);
    this.componentType = componentType;
    setTag(DefaultTag.PIGS.value());
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(
      getX() - DEFAULT_SIZE / 2, getY() - DEFAULT_SIZE / 2, DEFAULT_SIZE, DEFAULT_SIZE));}

  @Override
  public String getComponentType() {return componentType;}
}

