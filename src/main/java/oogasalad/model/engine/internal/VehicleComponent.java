package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class VehicleComponent extends GameComponent {

  public VehicleComponent(Pose pose, double mass) {
    super(pose, mass);
  }
}
