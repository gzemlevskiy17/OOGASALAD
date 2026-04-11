package oogasalad.model.engine.internal.components;

public abstract class DynamicComponent extends GameComponent {
  public DynamicComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
  }
}
