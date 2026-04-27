package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;

/**
 * @author Paulina Vvedenskaya
 */
public final class StructuralPartSpec extends AbstractVehiclePartSpec {

  private static final double DEFAULT_MASS = 1.5;
  private static final double FRICTION = 0.3;
  private static final double RESTITUTION = 0.5;
  private static final double MAX_HP = 100.0;

  private final double mass;

  public StructuralPartSpec(String paletteName, JointStyle jointStyle) {
    super(paletteName, jointStyle);
    this.mass = DEFAULT_MASS;
  }

  public StructuralPartSpec(String paletteName, JointStyle jointStyle,
      double mass, double width, double height) {
    super(paletteName, jointStyle, width, height);
    this.mass = mass;
  }

  @Override
  public GameComponent create(double x, double y) {
    ConvexPolygon collider = jointStyle() == JointStyle.WHEEL
        ? ConvexPolygon.regularPolygon(x, y, Math.min(bodyWidth(), bodyHeight()) / 2.0, 16, 0)
        : ConvexPolygon.rotatedBox(x, y, bodyWidth(), bodyHeight(), 0);
    return new StructuralComponent(
        x, y, 0,
        collider,
        "",
        FRICTION,
        mass,
        boxMomentOfInertia(mass, bodyWidth(), bodyHeight()),
        RESTITUTION,
        true,
        MAX_HP,
        paletteName(),
        bodyWidth(),
        bodyHeight());
  }
}
