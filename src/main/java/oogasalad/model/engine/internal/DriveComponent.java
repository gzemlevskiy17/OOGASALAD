package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class DriveComponent extends VehicleComponent {

  public DriveComponent(Pose pose, double mass, double friction, double maxHp) {
    super(pose, mass, friction, maxHp);
  }
}
