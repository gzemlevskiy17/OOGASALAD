package oogasalad.model.engine.internal.components;
import oogasalad.model.engine.api.DefaultTag;

public abstract class DynamicComponent extends GameComponent {
  public DynamicComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
  }

  @Override
  public String getTag() {
    return DefaultTag.DYNAMIC.value();
  }
}
