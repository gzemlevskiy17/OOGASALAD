package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Pose;

public class StructuralComponent extends VehicleComponent {

  public StructuralComponent(Pose pose, double mass, double friction, double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(pose, mass, friction, maxHp, displayName, spritePath, spriteWidth, spriteHeight);
  }
}
