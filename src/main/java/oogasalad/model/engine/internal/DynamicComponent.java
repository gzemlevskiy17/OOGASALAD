package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class DynamicComponent extends GameComponent {
  public DynamicComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }
}
