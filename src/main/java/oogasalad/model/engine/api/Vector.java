package oogasalad.model.engine.api;

/**
 * Immutable 2D vector with common operations used by rendering and physics code.
 *
 * <p>Use this record for positions, velocities, accelerations, normals, and other planar values
 * that need basic vector arithmetic.
 *
 * <p><strong>Assumptions:</strong> The record does not validate or clamp component values. Calls
 * such as {@link #normalize()} may produce {@code NaN} or infinities when given a zero-length
 * vector.
 *
 * <p><strong>Dependencies:</strong> Depends only on {@link Math} and implements
 * {@link Comparable} by vector magnitude.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * Vector position = new Vector(3, 4);
 * Vector unitNormal = position.normalize();
 * double distance = position.magnitude();
 * }</pre>
 *
 * @param x x component of the vector
 * @param y y component of the vector
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
    double mag = magnitude();
    if (mag == 0) {
      return new Vector(0, 0);
    }
    return new Vector(this.x() / mag, this.y() / mag);
  }

  public double dot(Vector other) {
    return this.x() * other.x() + this.y() * other.y();
  }

  public Vector subtract(Vector other) {
    return new Vector(this.x() - other.x(), this.y() - other.y());
  }

  /**
   * Rotates this vector by an arbitrary angle in radians.
   *
   * @param radians counterclockwise rotation angle
   * @return rotated vector
   */
  public Vector rotate(double radians) {
    double cosine = Math.cos(radians);
    double sine = Math.sin(radians);
    return new Vector(
        (this.x() * cosine) - (this.y() * sine),
        (this.x() * sine) + (this.y() * cosine)
    );
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
   * Computes the 2D scalar cross product with another vector.
   *
   * @param other other vector
   * @return scalar cross product ({@code x1*y2 - y1*x2})
   */
  public double crossWithVector(Vector other) {
    return this.x() * other.y() - this.y() * other.x();
  }

  /**
   * Used with the scalar as an angular speed and returns the perpendicular linear component.
   *
   * @param factor scalar multiplier, typically an angular velocity or acceleration value
   * @return a vector perpendicular to this one and scaled by {@code factor}
   */
  public Vector crossWithScalar(double factor) {
    return new Vector(-1 * this.y() * factor, this.x() * factor);
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
