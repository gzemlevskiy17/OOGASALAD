package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;

/**
 * Concrete dynamic component tagged as pigs.
 */
public class PigComponent extends DynamicComponent {

  public PigComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity);
    setTag(DefaultTag.PIGS.value());
  }

  /**
   * @param other
   */
  @Override
  public void collideWith(GameObject other) {

  }

  /**
   * @param component
   */
  @Override
  public void handleCollision(StaticComponent component) {

  }

  /**
   * @param component
   */
  @Override
  public void handleCollision(DynamicComponent component) {

  }
}

