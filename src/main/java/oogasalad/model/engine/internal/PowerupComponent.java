package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;

public abstract class PowerupComponent extends GameComponent {
  private boolean triggerConditionSatisfied;

  public PowerupComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
    setTrigger(true);
    this.triggerConditionSatisfied = true;
    setAffectedByGravity(false);
  }

  public boolean isTriggerConditionSatisfied() {
    return triggerConditionSatisfied;
  }

  public void setTriggerConditionSatisfied(boolean triggerConditionSatisfied) {
    this.triggerConditionSatisfied = triggerConditionSatisfied;
  }

  public abstract void applyEffect(GameObject target);
}
