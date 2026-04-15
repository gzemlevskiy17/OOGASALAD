package oogasalad.model.engine.internal.components;

/**
 * @author Paulina Vvedenskaya
 */
public class DriveComponent extends VehicleComponent {

  public DriveComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(x, y, rotation, mass, friction, maxHp, displayName, spritePath, spriteWidth,
        spriteHeight);
  }
}
