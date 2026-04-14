package oogasalad.model.engine.internal.components;

public abstract class StaticComponent extends GameComponent{
  public StaticComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
  }

  @Override
  public String getTag() {
    return "static";
  }
}
