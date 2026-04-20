package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;

/**
 * @author Paulina Vvedenskaya
 */
public class DriveComponent extends VehicleComponent {

  public DriveComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity,
      double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity, maxHp, displayName, spritePath, spriteWidth,
        spriteHeight);
  }
}
