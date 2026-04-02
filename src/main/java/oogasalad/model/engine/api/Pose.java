package oogasalad.model.engine.api;

/** Immutable position and orientation record for physics and rendering. */
public record Pose(double x, double y, double rotation) {

  public Pose {
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(rotation)) {
      throw new IllegalArgumentException("Pose values must be valid numbers");
    }
  }
}
