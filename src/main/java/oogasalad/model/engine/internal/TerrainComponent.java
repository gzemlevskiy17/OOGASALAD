package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

public abstract class TerrainComponent extends GameComponent {
  private final boolean lethal;

  public TerrainComponent(Pose pose, double mass, double friction, boolean lethal) {
    super(pose, mass, friction);
    this.lethal = lethal;
  }

  public boolean isLethal() {
    return lethal;
  }
}
