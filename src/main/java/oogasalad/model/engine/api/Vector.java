package oogasalad.model.engine.api;

/** Immutable X/Y representation for physics and spatial math. */
public record Vector(double x, double y) {

  public Vector add(Vector other) {
    return new Vector(x + other.x, y + other.y);
  }
}
