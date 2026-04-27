package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;

/**
 * @author Paulina Vvedenskaya
 */
public final class PigPartSpec extends AbstractVehiclePartSpec {

  private static final double DEFAULT_MASS = 2.0;
  private static final double FRICTION = 0.3;
  private static final double RESTITUTION = 0.5;
  private static final double MAX_HP = 100.0;

  private final double mass;

  public PigPartSpec(String paletteName) {
    super(paletteName, JointStyle.PIG);
    this.mass = DEFAULT_MASS;
  }

  public PigPartSpec(String paletteName, double mass, double width, double height) {
    super(paletteName, JointStyle.PIG, width, height);
    this.mass = mass;
  }

  @Override
  public GameComponent create(double x, double y) {
    return new PigComponent(
        x, y, 0,
        ConvexPolygon.axisAlignedBox(x - bodyWidth() / 2, y - bodyHeight() / 2, bodyWidth(),
            bodyHeight()),
        "",
        FRICTION,
        mass,
        boxMomentOfInertia(mass, bodyWidth(), bodyHeight()),
        RESTITUTION,
        true,
        MAX_HP,
        paletteName());
  }
}
