package oogasalad.model.engine.internal.components;

/**
 * @author Paulina Vvedenskaya
 */
public class StructuralComponent extends VehicleComponent {

  public StructuralComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(x, y, rotation, mass, friction, maxHp, displayName, spritePath, spriteWidth,
        spriteHeight);
  }

  public StructuralComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp,
      String displayName, String spritePath, String hubSpritePath, String wheelSpritePath,
      double spriteWidth, double spriteHeight) {
    super(x, y, rotation, mass, friction, maxHp, displayName, spritePath, hubSpritePath,
        wheelSpritePath, spriteWidth, spriteHeight);
  }
}
