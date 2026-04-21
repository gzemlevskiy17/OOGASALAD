package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;

/**
 * @author Paulina Vvedenskaya
 */
public class StructuralComponent extends VehicleComponent {

<<<<<<< HEAD
  public StructuralComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity,
      double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity, maxHp, displayName, spritePath, spriteWidth,
        spriteHeight);
=======
  public StructuralComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp, String displayName, double bodyWidth, double bodyHeight) {
    super(x, y, rotation, mass, friction, maxHp, displayName, bodyWidth, bodyHeight);
>>>>>>> origin/feat/dev/joints-and-vehicle-build
  }

  @Override
  public boolean isTrigger() {
    return false;
  }
}
