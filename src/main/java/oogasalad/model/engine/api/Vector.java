package oogasalad.model.engine.api;

//TODO: complete javadoc

/**
 * Immutable
 *
 * @param x
 * @param y
 * @author Georgiy Zemlevskiy
 */
public record Vector(double x, double y) {

  public Vector multiply(double factor) {
    return new Vector(this.x() * factor, this.y() * factor);
  }

  public Vector add(Vector other) {
    return new Vector(this.x() + other.x(), this.y() + other.y());
  }

  public double magnitude() {
    return Math.sqrt(Math.pow(this.x(), 2) + Math.pow(this.y(), 2));
  }

  public Vector normalize() {
    return new Vector(this.x() / magnitude(), this.y() / magnitude());
  }
}