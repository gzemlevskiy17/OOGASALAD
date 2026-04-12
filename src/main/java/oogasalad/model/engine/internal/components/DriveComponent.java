package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.Pose;

public abstract class DriveComponent extends VehicleComponent {

  public DriveComponent(Pose pose, double mass, double friction, double maxHp,
      String displayName, String spritePath, double spriteWidth, double spriteHeight) {
    super(pose, mass, friction, maxHp, displayName, spritePath, spriteWidth, spriteHeight);
  }

  public abstract void activate();
}
