package oogasalad.model.engine.internal.components;
import oogasalad.model.engine.api.DefaultTag;

/**
 * @author Xuye
 */

public abstract class FinishLineComponent extends PassthroughComponent {

  public FinishLineComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTag(DefaultTag.FINISH_LINE.value());
    setTrigger(true);
  }
}
