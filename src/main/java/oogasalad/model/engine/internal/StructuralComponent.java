package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class StructuralComponent extends VehicleComponent {

  public StructuralComponent(Pose pose, double mass) {
    super(pose, mass);
  }
}
