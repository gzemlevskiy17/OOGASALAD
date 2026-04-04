package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

/**
 * @author Xuye
 */
public abstract class TerrainComponent extends StaticComponent {
  public TerrainComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }

  public abstract double getWidth();

  public abstract double getHeight();

  public abstract String getSprite();

  public abstract boolean isTiled();
}
