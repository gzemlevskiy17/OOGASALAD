package oogasalad.model.engine.internal.components;

/**
 * @author Xuye
 */

public abstract class FinishLineComponent extends PassthroughComponent {

  public FinishLineComponent(double x, double y, double rotation, double mass, double friction) {
    super(x, y, rotation, mass, friction);
    setTrigger(true);
  }
}
