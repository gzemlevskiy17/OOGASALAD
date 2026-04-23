package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;

/**
 * @author Paulina Vvedenskaya
 */
public final class StructuralPartSpec extends AbstractVehiclePartSpec {

  private static final double MASS = 1.5;
  private static final double FRICTION = 0.3;
  private static final double RESTITUTION = 0.5;
  private static final double MAX_HP = 100.0;

  public StructuralPartSpec(String paletteName, JointStyle jointStyle) {
    super(paletteName, jointStyle);
  }

  @Override
  public GameComponent create(double x, double y) {
    return new StructuralComponent(
        x, y, 0,
        ConvexPolygon.axisAlignedBox(x, y, bodyWidth(), bodyHeight()),
        "",
        FRICTION,
        MASS,
        boxMomentOfInertia(MASS, bodyWidth(), bodyHeight()),
        RESTITUTION,
        true,
        MAX_HP,
        paletteName(),
        bodyWidth(),
        bodyHeight());
  }
}
