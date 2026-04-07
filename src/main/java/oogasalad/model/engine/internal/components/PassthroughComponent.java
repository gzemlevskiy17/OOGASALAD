package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;

public abstract class PassthroughComponent extends GameComponent {

  public PassthroughComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
    setTrigger(true);
    setAffectedByGravity(false);
  }

  public abstract void applyEffect(GameObject target);
}
