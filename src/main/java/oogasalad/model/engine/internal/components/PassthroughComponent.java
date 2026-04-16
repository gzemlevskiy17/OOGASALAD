package oogasalad.model.engine.internal.components;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;

public abstract class PassthroughComponent extends GameComponent {

  public PassthroughComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTag(DefaultTag.PASSTHROUGH.value());
    setTrigger(true);
    setAffectedByGravity(false);
  }

  public abstract void applyEffect(GameObject target);
}
