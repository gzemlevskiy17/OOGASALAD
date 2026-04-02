package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;

public abstract class PowerupComponent extends GameComponent {

  public PowerupComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
    setTrigger(true);
    setAffectedByGravity(false);
  }

  public abstract void applyEffect(GameObject target);
}
