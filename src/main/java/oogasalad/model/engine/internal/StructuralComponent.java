package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

/**
 * @author Xuye
 */

public abstract class StructuralComponent extends VehicleComponent {

  public StructuralComponent(Pose pose, double mass, double friction, double maxHp) {
    super(pose, mass, friction, maxHp);
  }
}
