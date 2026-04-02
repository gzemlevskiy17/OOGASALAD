package oogasalad.model.engine.api;

/**
 * Immutable 2D vector with common operations used by rendering and physics code.
 *
 * <p>Use this record for positions, velocities, normals, and force values across model and engine
 * packages.
 *
 * <p><strong>Assumptions:</strong> Methods do not clamp or sanitize values; operations can produce
 * {@code NaN} or infinities if inputs are invalid (for example normalizing a zero vector).
 *
 * <p><strong>Dependencies:</strong> Depends only on {@link Math} and implements
 * {@link Comparable} by vector magnitude.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * Vector a = new Vector(3, 4);
 * Vector unit = a.normalize();
 * double speed = a.magnitude();
 * }</pre>
 *
 * @param x x component
 * @param y y component
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

  //TODO: handle 0 length vectors
  public Vector normalize() {
    return new Vector(this.x() / magnitude(), this.y() / magnitude());
  }

  public double dot(Vector other) {
    return this.x() * other.x() + this.y() * other.y();
  }

  public Vector subtract(Vector other) {
    return new Vector(this.x() - other.x(), this.y() - other.y());
  }

  //TODO: make more general method for rotations?
  /**
   * Rotates this vector by -90 degrees (clockwise).
   *
   * @return clockwise perpendicular vector
   */
  public Vector rotateRight() {
    return new Vector(this.y(), -1 * this.x());
  }

  /**
   * Rotates this vector by +90 degrees (counterclockwise).
   *
   * @return counterclockwise perpendicular vector
   */
  public Vector rotateLeft() {
    return new Vector(-this.y(), this.x());
  }

  /**
   * Computes 2D scalar cross product with another vector.
   *
   * @param other other vector
   * @return scalar cross product ({@code x1*y2 - y1*x2})
   */
  public double cross(Vector other) {
    return this.x() * other.y() - this.y() * other.x();
  }

  /**
   * Compares vectors by magnitude.
   *
   * @param other other vector
   * @return negative, zero, or positive based on magnitude comparison
   */
  @Override
  public int compareTo(Vector other) {
    return Double.compare(this.magnitude(), other.magnitude());
  }
}