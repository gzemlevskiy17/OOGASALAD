package oogasalad.model.engine.internal.components;

import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * @author Xuye
 */
public abstract class TerrainComponent extends StaticComponent implements AuthoringPlaceable {
  public TerrainComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
  }

  public abstract double getWidth();

  public abstract double getHeight();

  public abstract String getSprite();

  public abstract boolean isTiled();
}
