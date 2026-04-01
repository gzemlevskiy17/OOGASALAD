package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class FinishLineComponent extends GameComponent {
  public FinishLineComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }

  public boolean isVictoryTrigger() {
    return true;
  }
}
