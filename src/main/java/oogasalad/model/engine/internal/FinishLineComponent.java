package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

/**
 * @author Xuye
 */

public abstract class FinishLineComponent extends GameComponent {

  public FinishLineComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
    setTrigger(true);
  }
}
