package oogasalad.controller.player.internal.parts;

/**
 * @author Paulina Vvedenskaya
 */
abstract class AbstractVehiclePartSpec implements VehiclePartSpec {

  private final String paletteName;
  private final JointStyle jointStyle;
  private final double bodyWidth;
  private final double bodyHeight;

  AbstractVehiclePartSpec(String paletteName, JointStyle jointStyle) {
    this.paletteName = paletteName;
    this.jointStyle = jointStyle;
    this.bodyWidth = Palette.width(paletteName);
    this.bodyHeight = Palette.height(paletteName);
  }

  AbstractVehiclePartSpec(String paletteName, JointStyle jointStyle,
      double bodyWidth, double bodyHeight) {
    this.paletteName = paletteName;
    this.jointStyle = jointStyle;
    this.bodyWidth = bodyWidth;
    this.bodyHeight = bodyHeight;
  }

  static double boxMomentOfInertia(double mass, double width, double height) {
    return mass * (width * width + height * height) / 12.0;
  }

  @Override
  public final String paletteName() {
    return paletteName;
  }

  @Override
  public final JointStyle jointStyle() {
    return jointStyle;
  }

  @Override
  public final double bodyWidth() {
    return bodyWidth;
  }

  @Override
  public final double bodyHeight() {
    return bodyHeight;
  }
}
