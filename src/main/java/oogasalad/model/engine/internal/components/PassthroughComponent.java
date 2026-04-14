package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.GameObject;

public abstract class PassthroughComponent extends GameComponent {

  public PassthroughComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTrigger(true);
    setAffectedByGravity(false);
  }

  @Override
  public String getTag() {
    return "passthrough";
  }

  public abstract void applyEffect(GameObject target);
}
