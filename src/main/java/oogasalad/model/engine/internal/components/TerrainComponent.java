package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Base authoring-aware terrain component for static terrain objects in the engine.
 *
 * @author Xuye
 * @author Emilio Garcia
 */
public abstract class TerrainComponent extends StaticComponent implements AuthoringPlaceable {

  public TerrainComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double restitution) {
    super(x, y, rotation, polygon, tag, friction, restitution);
    setTag(DefaultTag.TERRAIN.value());
  }

  public abstract double getWidth();

  public abstract double getHeight();

  public abstract boolean isTiled();
}
