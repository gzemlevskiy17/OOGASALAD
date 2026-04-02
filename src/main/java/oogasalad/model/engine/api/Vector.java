package oogasalad.model.engine.api;

//TODO: complete javadoc

/**
 * Immutable
 *
 * @param x
 * @param y
 * @author Georgiy Zemlevskiy
 */
public record Vector(double x, double y) implements Comparable<Vector> {

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

  public double dot(Vector other) {
    return this.x() * other.x() + this.y() * other.y();
  }

  public Vector subtract(Vector other) {
    return new Vector(this.x() - other.x(), this.y() - other.y());
  }

  //TODO: make more general method?
  public Vector rotateRight() {
    return new Vector(this.y(), -1 * this.x());
  }

  public Vector rotateLeft() {
    return new Vector(-this.y(), this.x());
  }

  public Vector perpendicular() {
    return new Vector(-this.y(), this.x());
  }

  public double cross(Vector other) {
    return this.x() * other.y() - this.y() * other.x();
  }


  @Override
  public int compareTo(Vector other) {
    return Double.compare(this.magnitude(), other.magnitude());
  }
}