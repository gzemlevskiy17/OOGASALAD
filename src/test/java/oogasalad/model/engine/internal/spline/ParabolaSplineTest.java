package oogasalad.model.engine.internal.spline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ParabolaSpline}.
 *
 * <p>Focus: structural invariants (enough knots, knots above baseline, resolution ≥ 1), defensive
 * copying, midpoint-control derivation, and correctness of {@link ParabolaSpline#evaluate} on a
 * hand-verifiable Bézier segment.
 */
class ParabolaSplineTest {

  private static final double BASELINE = 100.0;
  private static final double TOLERANCE = 1e-9;

  @Test
  void constructor_fewerThanTwoKnots_throws() {
    List<Vector> knots = List.of(new Vector(0, 10));
    assertThrows(IllegalArgumentException.class,
        () -> new ParabolaSpline(knots, BASELINE, 4));
  }

  @Test
  void constructor_knotOnOrBelowBaseline_throws() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, BASELINE));
    assertThrows(IllegalArgumentException.class,
        () -> new ParabolaSpline(knots, BASELINE, 4));
  }

  @Test
  void constructor_nonPositiveResolution_throws() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10));
    assertThrows(IllegalArgumentException.class,
        () -> new ParabolaSpline(knots, BASELINE, 0));
  }

  @Test
  void constructor_explicitControlsWithWrongCount_throws() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10), new Vector(20, 10));
    List<Vector> controls = List.of(new Vector(5, 5)); // should have 2 controls, not 1
    assertThrows(IllegalArgumentException.class,
        () -> new ParabolaSpline(knots, controls, BASELINE, 4));
  }

  @Test
  void constructor_defensiveCopyOfKnots_mutationDoesNotLeakIntoSpline() {
    List<Vector> knots = new ArrayList<>(List.of(new Vector(0, 10), new Vector(10, 10)));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);
    knots.add(new Vector(99, 99));
    assertEquals(2, spline.knots().size());
  }

  @Test
  void defaultControls_areMidpointsBetweenAdjacentKnots() {
    List<Vector> knots = List.of(new Vector(0, 20), new Vector(10, 40));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);

    List<Vector> controls = spline.controls();
    assertEquals(1, controls.size());
    assertEquals(5.0, controls.get(0).x(), TOLERANCE);
    assertEquals(30.0, controls.get(0).y(), TOLERANCE);
  }

  @Test
  void evaluate_atEndpoints_returnsKnots() {
    List<Vector> knots = List.of(new Vector(0, 20), new Vector(10, 40));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);

    Vector start = spline.evaluate(0, 0.0);
    Vector end = spline.evaluate(0, 1.0);

    assertEquals(0.0, start.x(), TOLERANCE);
    assertEquals(20.0, start.y(), TOLERANCE);
    assertEquals(10.0, end.x(), TOLERANCE);
    assertEquals(40.0, end.y(), TOLERANCE);
  }

  @Test
  void evaluate_withMidpointControls_producesStraightLine() {
    // With control as midpoint of the two knots, the quadratic collapses to a straight line.
    List<Vector> knots = List.of(new Vector(0, 20), new Vector(10, 40));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);

    Vector mid = spline.evaluate(0, 0.5);
    assertEquals(5.0, mid.x(), TOLERANCE);
    assertEquals(30.0, mid.y(), TOLERANCE);
  }

  @Test
  void evaluate_withCustomControl_followsBezierFormula() {
    // P(t) = (1-t)^2 P0 + 2(1-t)t P1 + t^2 P2; at t=0.5 with P0=(0,0), P1=(0,40), P2=(20,0) we
    // expect (5, 20) — the curve bows up toward the control.
    List<Vector> knots = List.of(new Vector(0, 0), new Vector(20, 0));
    List<Vector> controls = List.of(new Vector(0, 40));
    ParabolaSpline spline = new ParabolaSpline(knots, controls, BASELINE, 4);

    Vector mid = spline.evaluate(0, 0.5);
    assertEquals(5.0, mid.x(), TOLERANCE);
    assertEquals(20.0, mid.y(), TOLERANCE);
  }

  @Test
  void segmentCount_equalsKnotsMinusOne() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10), new Vector(20, 10),
        new Vector(30, 10));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);
    assertEquals(3, spline.segmentCount());
  }

  @Test
  void withResolution_returnsNewInstance_withSameKnotsAndBaseline() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10));
    ParabolaSpline original = new ParabolaSpline(knots, BASELINE, 4);
    ParabolaSpline scaled = original.withResolution(32);

    assertNotSame(original, scaled);
    assertEquals(32, scaled.segmentsPerKnotPair());
    assertEquals(original.knots(), scaled.knots());
    assertEquals(original.baselineY(), scaled.baselineY(), TOLERANCE);
  }

  @Test
  void knots_returnedListIsUnmodifiable() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10));
    ParabolaSpline spline = new ParabolaSpline(knots, BASELINE, 4);

    assertThrows(UnsupportedOperationException.class,
        () -> spline.knots().add(new Vector(99, 99)));
  }

  @Test
  void equality_isStructural() {
    List<Vector> knots = List.of(new Vector(0, 10), new Vector(10, 10));
    ParabolaSpline a = new ParabolaSpline(knots, BASELINE, 4);
    ParabolaSpline b = new ParabolaSpline(knots, BASELINE, 4);

    assertTrue(a.equals(b));
    assertEquals(a.hashCode(), b.hashCode());
  }
}
