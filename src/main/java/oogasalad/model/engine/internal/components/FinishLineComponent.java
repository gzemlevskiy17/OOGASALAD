package oogasalad.model.engine.internal.components;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;

/**
 * @author Xuye
 * @author Iurii Beliaev
 */

public abstract class FinishLineComponent extends PassThroughComponent {

  protected FinishLineComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag) {
    super(x, y, rotation, polygon, tag);
    setTag(DefaultTag.FINISH_LINE.value());
  }
}
