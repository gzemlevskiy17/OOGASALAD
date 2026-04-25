package oogasalad.model.engine.internal.spline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import oogasalad.model.engine.api.Vector;

/**
 * Immutable description of a chain of quadratic Bézier ("parabola") segments used as authored
 * terrain. A spline is a list of knots connected pairwise by Bézier segments; each segment has one
 * explicit control point stored at the matching index in {@link #controls()}.
 *
 * <p>The spline is treated as a ground surface: it lives above a horizontal baseline at
 * {@link #baselineY()} (in world pixels, Y-down) and the region between the curve and the baseline
 * is what gets tessellated into physics polygons by {@link SplineTessellator}. As a result the
 * invariant "every knot has {@code y &lt; baselineY}" is enforced at construction time; otherwise
 * the tessellated slabs could degenerate or self-intersect.
 *
 * <p>The {@link #segmentsPerKnotPair()} value is the tessellation resolution: each Bézier segment
 * is sampled into this many sub-intervals before being handed to the physics engine, so higher
 * values produce smoother ground at a proportional collision cost.
 *
 * <p>This class is a value type. Lists passed in are defensively copied and exposed as unmodifiable
 * views; equality and hashing follow structural semantics.
 *
 * @author Bad Piggies team
 */
public final class ParabolaSpline {

  private static final int MIN_KNOTS = 2;
  private static final int MIN_SEGMENTS_PER_KNOT_PAIR = 1;

  private final List<Vector> knots;
  private final List<Vector> controls;
  private final double baselineY;
  private final int segmentsPerKnotPair;

  /**
   * @param knots               at least two distinct points defining the chain
   * @param controls            one control point per adjacent knot pair; {@code null} means
   *                            derive defaults as arithmetic midpoints (producing straight
   *                            segments)
   * @param baselineY           ground reference Y (world pixels); must be strictly greater than
   *                            every knot's Y
   * @param segmentsPerKnotPair tessellation resolution (&ge; 1) applied uniformly to all segments
   * @throws IllegalArgumentException when any structural invariant is violated
   */
  public ParabolaSpline(List<Vector> knots, List<Vector> controls, double baselineY,
      int segmentsPerKnotPair) {
    Objects.requireNonNull(knots, "knots");
    if (knots.size() < MIN_KNOTS) {
      throw new IllegalArgumentException("spline needs at least " + MIN_KNOTS + " knots");
    }
    if (segmentsPerKnotPair < MIN_SEGMENTS_PER_KNOT_PAIR) {
      throw new IllegalArgumentException(
          "segmentsPerKnotPair must be >= " + MIN_SEGMENTS_PER_KNOT_PAIR);
    }
    for (Vector k : knots) {
      if (k.y() >= baselineY) {
        throw new IllegalArgumentException(
            "every knot must sit strictly above baselineY (y < " + baselineY + "); got " + k);
      }
    }

    this.knots = List.copyOf(knots);
    this.baselineY = baselineY;
    this.segmentsPerKnotPair = segmentsPerKnotPair;

    int expectedControls = this.knots.size() - 1;
    List<Vector> resolved = (controls == null)
        ? deriveMidpointControls(this.knots)
        : new ArrayList<>(controls);
    if (resolved.size() != expectedControls) {
      throw new IllegalArgumentException(
          "controls must have exactly " + expectedControls + " entries (knots - 1)");
    }
    this.controls = Collections.unmodifiableList(resolved);
  }

  /**
   * Convenience overload that derives control points as the midpoints between adjacent knots,
   * producing straight-line segments at tessellation time.
   */
  public ParabolaSpline(List<Vector> knots, double baselineY, int segmentsPerKnotPair) {
    this(knots, null, baselineY, segmentsPerKnotPair);
  }

  public List<Vector> knots() {
    return knots;
  }

  public List<Vector> controls() {
    return controls;
  }

  public double baselineY() {
    return baselineY;
  }

  public int segmentsPerKnotPair() {
    return segmentsPerKnotPair;
  }

  /**
   * Evaluates the Bézier point at parameter {@code t} on the segment between knot {@code i} and
   * knot {@code i + 1}.
   *
   * @param segmentIndex zero-based index of the segment
   * @param t            parameter in {@code [0, 1]}
   * @return world-space point on the spline
   */
  public Vector evaluate(int segmentIndex, double t) {
    Vector p0 = knots.get(segmentIndex);
    Vector p1 = controls.get(segmentIndex);
    Vector p2 = knots.get(segmentIndex + 1);
    double oneMinusT = 1.0 - t;
    double x = oneMinusT * oneMinusT * p0.x()
        + 2.0 * oneMinusT * t * p1.x()
        + t * t * p2.x();
    double y = oneMinusT * oneMinusT * p0.y()
        + 2.0 * oneMinusT * t * p1.y()
        + t * t * p2.y();
    return new Vector(x, y);
  }

  /** Number of Bézier segments in this spline ({@code knots - 1}). */
  public int segmentCount() {
    return knots.size() - 1;
  }

  /**
   * Returns a new spline with the given resolution. Convenience for authoring-time edits that only
   * change tessellation density.
   */
  public ParabolaSpline withResolution(int newSegmentsPerKnotPair) {
    return new ParabolaSpline(knots, controls, baselineY, newSegmentsPerKnotPair);
  }

  private static List<Vector> deriveMidpointControls(List<Vector> knots) {
    List<Vector> mids = new ArrayList<>(knots.size() - 1);
    for (int i = 0; i < knots.size() - 1; i++) {
      Vector a = knots.get(i);
      Vector b = knots.get(i + 1);
      mids.add(new Vector(0.5 * (a.x() + b.x()), 0.5 * (a.y() + b.y())));
    }
    return mids;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParabolaSpline other)) return false;
    return Double.compare(other.baselineY, baselineY) == 0
        && segmentsPerKnotPair == other.segmentsPerKnotPair
        && knots.equals(other.knots)
        && controls.equals(other.controls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(knots, controls, baselineY, segmentsPerKnotPair);
  }

  @Override
  public String toString() {
    return "ParabolaSpline{knots=" + knots + ", controls=" + controls
        + ", baselineY=" + baselineY + ", resolution=" + segmentsPerKnotPair + "}";
  }
}
