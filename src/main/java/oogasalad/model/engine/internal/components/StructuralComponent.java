package oogasalad.model.engine.internal.components;

/**
 * @author Paulina Vvedenskaya
 */
public class StructuralComponent extends VehicleComponent {

  public StructuralComponent(double x, double y, double rotation, double mass, double friction,
      double maxHp, String displayName, double bodyWidth, double bodyHeight) {
    super(x, y, rotation, mass, friction, maxHp, displayName, bodyWidth, bodyHeight);
  }
}
