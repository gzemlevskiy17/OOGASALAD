package oogasalad.model.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VectorTest {

  private static final double EPS = 1e-9;

  // ── add ───────────────────────────────────────────────────────────────────

  @Test
  void add_sumsBothComponents() {
    Vector result = new Vector(1.0, 2.0).add(new Vector(3.0, 4.0));
    assertEquals(4.0, result.x(), EPS);
    assertEquals(6.0, result.y(), EPS);
  }

  @Test
  void add_zeroVectorIsIdentity() {
    Vector v = new Vector(5.0, -3.0);
    Vector result = v.add(new Vector(0, 0));
    assertEquals(v.x(), result.x(), EPS);
    assertEquals(v.y(), result.y(), EPS);
  }

  @Test
  void add_negativeComponents() {
    Vector result = new Vector(-2.0, -3.0).add(new Vector(-1.0, -4.0));
    assertEquals(-3.0, result.x(), EPS);
    assertEquals(-7.0, result.y(), EPS);
  }

  // ── subtract ─────────────────────────────────────────────────────────────

  @Test
  void subtract_differenceOfComponents() {
    Vector result = new Vector(5.0, 8.0).subtract(new Vector(2.0, 3.0));
    assertEquals(3.0, result.x(), EPS);
    assertEquals(5.0, result.y(), EPS);
  }

  @Test
  void subtract_selfYieldsZeroVector() {
    Vector v = new Vector(7.0, -4.0);
    Vector result = v.subtract(v);
    assertEquals(0.0, result.x(), EPS);
    assertEquals(0.0, result.y(), EPS);
  }

  // ── multiply ──────────────────────────────────────────────────────────────

  @Test
  void multiply_scalesComponents() {
    Vector result = new Vector(3.0, -2.0).multiply(4.0);
    assertEquals(12.0, result.x(), EPS);
    assertEquals(-8.0, result.y(), EPS);
  }

  @Test
  void multiply_byZeroYieldsZeroVector() {
    Vector result = new Vector(99.0, 99.0).multiply(0.0);
    assertEquals(0.0, result.x(), EPS);
    assertEquals(0.0, result.y(), EPS);
  }

  @Test
  void multiply_byNegativeFlipsSign() {
    Vector result = new Vector(2.0, -5.0).multiply(-1.0);
    assertEquals(-2.0, result.x(), EPS);
    assertEquals(5.0, result.y(), EPS);
  }

  // ── magnitude ─────────────────────────────────────────────────────────────

  @Test
  void magnitude_345Triangle() {
    assertEquals(5.0, new Vector(3.0, 4.0).magnitude(), EPS);
  }

  @Test
  void magnitude_zeroVector() {
    assertEquals(0.0, new Vector(0.0, 0.0).magnitude(), EPS);
  }

  @Test
  void magnitude_negativeComponents() {
    // magnitude is always non-negative
    assertEquals(5.0, new Vector(-3.0, -4.0).magnitude(), EPS);
  }

  @Test
  void magnitude_unitAxisVectors() {
    assertEquals(1.0, new Vector(1.0, 0.0).magnitude(), EPS);
    assertEquals(1.0, new Vector(0.0, 1.0).magnitude(), EPS);
  }

  // ── normalize ─────────────────────────────────────────────────────────────

  @Test
  void normalize_producesUnitVector() {
    Vector unit = new Vector(3.0, 4.0).normalize();
    assertEquals(1.0, unit.magnitude(), 1e-9);
  }

  @Test
  void normalize_directionPreserved() {
    Vector v = new Vector(3.0, 4.0);
    Vector unit = v.normalize();
    // unit and v must point the same direction: cross product == 0
    assertEquals(0.0, v.crossWithVector(unit), 1e-9);
    // and dot product is positive
    assertTrue(v.dot(unit) > 0);
  }

  @Test
  void normalize_zeroVector_producesZeroVector() {
    Vector unit = new Vector(0.0, 0.0).normalize();
    assertEquals(0.0, unit.x(), EPS);
    assertEquals(0.0, unit.y(), EPS);
  }

  // ── dot ───────────────────────────────────────────────────────────────────

  @Test
  void dot_orthogonalVectors_isZero() {
    assertEquals(0.0, new Vector(1.0, 0.0).dot(new Vector(0.0, 1.0)), EPS);
  }

  @Test
  void dot_parallelVectors_isProductOfMagnitudes() {
    Vector a = new Vector(2.0, 0.0);
    Vector b = new Vector(3.0, 0.0);
    assertEquals(6.0, a.dot(b), EPS);
  }

  @Test
  void dot_antiParallelVectors_isNegative() {
    assertTrue(new Vector(1.0, 0.0).dot(new Vector(-1.0, 0.0)) < 0);
  }

  @Test
  void dot_isCommutative() {
    Vector a = new Vector(2.0, 3.0);
    Vector b = new Vector(4.0, -1.0);
    assertEquals(a.dot(b), b.dot(a), EPS);
  }

  // ── crossWithVector ───────────────────────────────────────────────────────

  @Test
  void crossWithVector_xHatCrossYHat_isOne() {
    assertEquals(1.0, new Vector(1.0, 0.0).crossWithVector(new Vector(0.0, 1.0)), EPS);
  }

  @Test
  void crossWithVector_isAntiCommutative() {
    Vector a = new Vector(2.0, 3.0);
    Vector b = new Vector(4.0, -1.0);
    assertEquals(-a.crossWithVector(b), b.crossWithVector(a), EPS);
  }

  @Test
  void crossWithVector_parallelVectors_isZero() {
    assertEquals(0.0, new Vector(1.0, 2.0).crossWithVector(new Vector(2.0, 4.0)), EPS);
  }

  // ── crossWithScalar ───────────────────────────────────────────────────────

  @Test
  void crossWithScalar_producesPerpendicularScaledVector() {
    Vector v = new Vector(1.0, 0.0);
    Vector result = v.crossWithScalar(2.0);
    // cross(r, ω) = (-y*ω, x*ω)
    assertEquals(0.0, result.x(), EPS);
    assertEquals(2.0, result.y(), EPS);
  }

  @Test
  void crossWithScalar_byZero_yieldsZeroVector() {
    Vector result = new Vector(3.0, 7.0).crossWithScalar(0.0);
    assertEquals(0.0, result.x(), EPS);
    assertEquals(0.0, result.y(), EPS);
  }

  // ── rotate ────────────────────────────────────────────────────────────────

  @Test
  void rotate_byZeroRadians_unchanged() {
    Vector v = new Vector(2.0, 3.0);
    Vector result = v.rotate(0.0);
    assertEquals(v.x(), result.x(), EPS);
    assertEquals(v.y(), result.y(), EPS);
  }

  @Test
  void rotate_90DegreesCCW_xHatBecomesYHat() {
    Vector result = new Vector(1.0, 0.0).rotate(Math.PI / 2.0);
    assertEquals(0.0, result.x(), EPS);
    assertEquals(1.0, result.y(), EPS);
  }

  @Test
  void rotate_180Degrees_negatesVector() {
    Vector v = new Vector(3.0, 4.0);
    Vector result = v.rotate(Math.PI);
    assertEquals(-v.x(), result.x(), EPS);
    assertEquals(-v.y(), result.y(), EPS);
  }

  @Test
  void rotate_360Degrees_returnsToOrigin() {
    Vector v = new Vector(5.0, -2.0);
    Vector result = v.rotate(2 * Math.PI);
    assertEquals(v.x(), result.x(), EPS);
    assertEquals(v.y(), result.y(), EPS);
  }

  @Test
  void rotate_preservesMagnitude() {
    Vector v = new Vector(3.0, 4.0);
    assertEquals(v.magnitude(), v.rotate(1.23).magnitude(), EPS);
  }

  // ── rotateLeft / rotateRight ──────────────────────────────────────────────

  @Test
  void rotateLeft_xHatYieldsYHat() {
    Vector result = new Vector(1.0, 0.0).rotateLeft();
    assertEquals(0.0, result.x(), EPS);
    assertEquals(1.0, result.y(), EPS);
  }

  @Test
  void rotateRight_xHatYieldsNegativeYHat() {
    Vector result = new Vector(1.0, 0.0).rotateRight();
    assertEquals(0.0, result.x(), EPS);
    assertEquals(-1.0, result.y(), EPS);
  }

  @Test
  void rotateLeft_thenRotateRight_returnsOriginal() {
    Vector v = new Vector(3.0, 7.0);
    Vector result = v.rotateLeft().rotateRight();
    assertEquals(v.x(), result.x(), EPS);
    assertEquals(v.y(), result.y(), EPS);
  }

  @Test
  void rotateLeft_fourTimes_returnsOriginal() {
    Vector v = new Vector(3.0, 7.0);
    Vector result = v.rotateLeft().rotateLeft().rotateLeft().rotateLeft();
    assertEquals(v.x(), result.x(), EPS);
    assertEquals(v.y(), result.y(), EPS);
  }

  // ── compareTo ────────────────────────────────────────────────────────────

  @Test
  void compareTo_largerMagnitudeIsGreater() {
    assertTrue(new Vector(3.0, 4.0).compareTo(new Vector(1.0, 0.0)) > 0);
  }

  @Test
  void compareTo_smallerMagnitudeIsLess() {
    assertTrue(new Vector(1.0, 0.0).compareTo(new Vector(3.0, 4.0)) < 0);
  }

  @Test
  void compareTo_equalMagnitudesReturnZero() {
    assertEquals(0, new Vector(3.0, 0.0).compareTo(new Vector(0.0, 3.0)));
  }

  @Test
  void compareTo_sameVectorReturnsZero() {
    Vector v = new Vector(2.0, 5.0);
    assertEquals(0, v.compareTo(v));
  }

  // ── immutability ─────────────────────────────────────────────────────────

  @Test
  void operations_doNotMutateOriginal() {
    Vector original = new Vector(1.0, 2.0);
    original.add(new Vector(10.0, 10.0));
    original.multiply(99.0);
    original.normalize();
    // original must be unchanged
    assertEquals(1.0, original.x(), EPS);
    assertEquals(2.0, original.y(), EPS);
  }
}
