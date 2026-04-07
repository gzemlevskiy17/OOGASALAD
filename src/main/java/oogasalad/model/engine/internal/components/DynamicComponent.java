package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Pose;

public abstract class DynamicComponent extends GameComponent {
  public DynamicComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }
}
