package oogasalad.model.engine.internal.spline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SplineTessellator}.
 *
 * <p>Focus:
 * <ul>
 *   <li>Slab count matches {@code segments × resolution} for non-degenerate splines.</li>
 *   <li>Slab bottom edge sits {@code slabThickness} below the top edge (not at baseline).</li>
 *   <li>The overload with non-positive thickness is rejected.</li>
 *   <li>Degenerate sub-intervals (zero horizontal span) are skipped.</li>
 *   <li>Slabs inherit the spline's sampled Y, not the baseline — the regression guard for the
 *       "tall slab" ghost-collision bug.</li>
 * </ul>
 */
class SplineTessellatorTest {

  private static final double TOLERANCE = 1e-6;
  private static final double BASELINE = 1000.0;

  @Test
  void tessellate_straightLineWithResolutionOne_producesOneSlab() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    List<ConvexPolygon> slabs = SplineTessellator.tessellate(spline);

    assertEquals(1, slabs.size());
  }

  @Test
  void tessellate_defaultThickness_slabBottomIsShallow_notAtBaseline() {
    // Regression: slabs used to extend from the curve down to baselineY, creating sliver-thin
    // tall polygons that caused SAT ghost collisions. They should now be ~DEFAULT_SLAB_THICKNESS
    // pixels tall regardless of how far the baseline is.
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    ConvexPolygon slab = SplineTessellator.tessellate(spline).get(0);
    double maxY = slab.getPoints().stream().mapToDouble(Vector::y).max().orElseThrow();
    double minY = slab.getPoints().stream().mapToDouble(Vector::y).min().orElseThrow();

    assertEquals(SplineTessellator.DEFAULT_SLAB_THICKNESS, maxY - minY, TOLERANCE);
    assertFalse(maxY >= BASELINE, "slab must not reach the baseline");
  }

  @Test
  void tessellate_customThickness_respectsCallerValue() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    ConvexPolygon slab = SplineTessellator.tessellate(spline, 25.0).get(0);
    double maxY = slab.getPoints().stream().mapToDouble(Vector::y).max().orElseThrow();
    double minY = slab.getPoints().stream().mapToDouble(Vector::y).min().orElseThrow();

    assertEquals(25.0, maxY - minY, TOLERANCE);
  }

  @Test
  void tessellate_topEdgeEndpointsMatchSplineSamples() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 50)), BASELINE, 1);

    ConvexPolygon slab = SplineTessellator.tessellate(spline).get(0);

    // The two top points (minimum Y) should sit on the spline endpoints.
    List<Vector> points = slab.getPoints();
    double minY = points.stream().mapToDouble(Vector::y).min().orElseThrow();
    long tops = points.stream().filter(p -> Math.abs(p.y() - minY) < 50.0).count();
    assertTrue(tops >= 2);
    // Start and end knots are directly present in slab top corners.
    assertTrue(points.stream().anyMatch(p -> Math.abs(p.x()) < TOLERANCE && p.y() == 10.0));
    assertTrue(points.stream().anyMatch(p -> Math.abs(p.x() - 100) < TOLERANCE && p.y() == 50.0));
  }

  @Test
  void tessellate_resolutionGreaterThanOne_emitsOneSlabPerSubInterval() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 8);

    List<ConvexPolygon> slabs = SplineTessellator.tessellate(spline);

    assertEquals(8, slabs.size());
  }

  @Test
  void tessellate_multipleSegments_emitsResolutionSlabsPerSegment() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(50, 10), new Vector(100, 10)), BASELINE, 4);

    List<ConvexPolygon> slabs = SplineTessellator.tessellate(spline);

    assertEquals(8, slabs.size()); // 2 segments × 4 resolution
  }

  @Test
  void tessellate_zeroThickness_throws() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    assertThrows(IllegalArgumentException.class,
        () -> SplineTessellator.tessellate(spline, 0.0));
  }

  @Test
  void tessellate_negativeThickness_throws() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    assertThrows(IllegalArgumentException.class,
        () -> SplineTessellator.tessellate(spline, -5.0));
  }

  @Test
  void tessellate_verticalStackedKnots_degenerateSpanIsSkipped() {
    // Knots sharing the same X cause the midpoint control to also share that X, making every
    // sampled sub-interval horizontally degenerate. The tessellator must skip them rather than
    // emit zero-width polygons.
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(50, 10), new Vector(50, 30)), BASELINE, 4);

    List<ConvexPolygon> slabs = SplineTessellator.tessellate(spline);

    assertEquals(0, slabs.size());
  }

  @Test
  void tessellate_slabHasFivePointsForClosedClockwiseWinding() {
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(100, 10)), BASELINE, 1);

    ConvexPolygon slab = SplineTessellator.tessellate(spline).get(0);

    assertEquals(5, slab.getPoints().size());
    // First and last point coincide to close the polygon.
    assertEquals(slab.getPoints().get(0), slab.getPoints().get(4));
  }
}
