package oogasalad.model.engine.api;

/** Immutable X/Y representation for physics and spatial math. */
public record Vector(double x, double y) {
  public static final Vector ZERO = new Vector(0, 0);

  public Vector add(Vector other) {
    return new Vector(x + other.x, y + other.y);
  }

  public Vector scale(double factor) {
    return new Vector(x * factor, y * factor);
  }
}
