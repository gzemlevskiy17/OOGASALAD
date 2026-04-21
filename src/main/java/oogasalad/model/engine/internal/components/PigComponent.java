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

  public PigComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity) {
    this(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity, "Pig");
  }

  public PigComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity, String componentType) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity);
    this.componentType = componentType;
    setTag(DefaultTag.PIGS.value());
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(
        getX() - DEFAULT_SIZE / 2, getY() - DEFAULT_SIZE / 2, DEFAULT_SIZE, DEFAULT_SIZE));
  }

  @Override
  public String getComponentType() {
    return componentType;
  }

  @Override
  public boolean isTrigger() {
    return false;
  }
}
