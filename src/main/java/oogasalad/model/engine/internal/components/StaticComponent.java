package oogasalad.model.engine.internal.components;
import oogasalad.model.engine.api.DefaultTag;

public abstract class StaticComponent extends GameComponent{

  public StaticComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTag(DefaultTag.STATIC.value());
  }
}
