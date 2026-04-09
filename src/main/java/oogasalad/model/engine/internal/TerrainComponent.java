package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * @author Xuye
 */
public abstract class TerrainComponent extends StaticComponent implements AuthoringPlaceable {
  public TerrainComponent(Pose pose, double mass, double friction) {
    super(pose, mass, friction);
  }

  public abstract double getWidth();

  public abstract double getHeight();

  public abstract String getSprite();

  public abstract boolean isTiled();
}
