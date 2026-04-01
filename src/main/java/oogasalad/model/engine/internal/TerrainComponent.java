package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class TerrainComponent extends GameComponent {

  public TerrainComponent(Pose pose, double mass) {
    super(pose, mass);
  }
}
