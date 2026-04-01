package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class FinishLineComponent extends GameComponent {
  private boolean victoryConditionSatisfied;

  public FinishLineComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
    this.victoryConditionSatisfied = false;
  }

  public boolean isVictoryTrigger() {
    return true;
  }

  public boolean isVictoryConditionSatisfied() {
    return victoryConditionSatisfied;
  }

  public void setVictoryConditionSatisfied(boolean victoryConditionSatisfied) {
    this.victoryConditionSatisfied = victoryConditionSatisfied;
  }
}
