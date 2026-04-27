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
 * <p>Tessellation emits one slab per knot-to-knot segment. Curve fidelity for collision purposes
 * is therefore controlled by the density of authored knots, not by a separate sampling resolution.
 *
 * <p>This class is a value type. Lists passed in are defensively copied and exposed as
 * unmodifiable
 * views; equality and hashing follow structural semantics.
 *
 * @author Bad Piggies team
 */
public record ParabolaSpline(List<Vector> knots, List<Vector> controls, double baselineY,
    int resolution) {

  private static final int MIN_KNOTS = 2;

  /**
   * @param knots      at least two distinct points defining the chain
   * @param controls   one control point per adjacent knot pair; {@code null} means derive defaults
   *                   as arithmetic midpoints (producing straight segments)
   * @param baselineY  ground reference Y (world pixels); must be strictly greater than every knot's
   *                   Y
   * @param resolution number of tessellation slabs emitted per Bézier segment; must be ≥ 1
   * @throws IllegalArgumentException when any structural invariant is violated
   */
  public ParabolaSpline(List<Vector> knots, List<Vector> controls, double baselineY,
      int resolution) {
    Objects.requireNonNull(knots, "knots");
    if (knots.size() < MIN_KNOTS) {
      throw new IllegalArgumentException("spline needs at least " + MIN_KNOTS + " knots");
    }
    for (Vector k : knots) {
      if (k.y() >= baselineY) {
        throw new IllegalArgumentException(
            "every knot must sit strictly above baselineY (y < " + baselineY + "); got " + k);
      }
    }
    if (resolution < 1) {
      throw new IllegalArgumentException("resolution must be >= 1; got " + resolution);
    }

    this.knots = List.copyOf(knots);
    this.baselineY = baselineY;
    this.resolution = resolution;

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

  /** Convenience overload: explicit controls, resolution defaults to 1. */
  public ParabolaSpline(List<Vector> knots, List<Vector> controls, double baselineY) {
    this(knots, controls, baselineY, 1);
  }

  /** Convenience overload: derived controls, caller-specified resolution. */
  public ParabolaSpline(List<Vector> knots, double baselineY, int resolution) {
    this(knots, null, baselineY, resolution);
  }

  /**
   * Convenience overload that derives control points as the midpoints between adjacent knots,
   * producing straight-line segments at tessellation time.
   */
  public ParabolaSpline(List<Vector> knots, double baselineY) {
    this(knots, null, baselineY, 1);
  }

  /**
   * Evaluates the Bézier point at parameter {@code t} on the segment between knot {@code i} and
   * knot {@code i + 1}. Used by the renderer for visual sampling; physics tessellation uses the
   * knot endpoints directly.
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

  /**
   * Number of Bézier segments in this spline ({@code knots - 1}).
   */
  public int segmentCount() {
    return knots.size() - 1;
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
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParabolaSpline other)) {
      return false;
    }
    return Double.compare(other.baselineY, baselineY) == 0
        && knots.equals(other.knots)
        && controls.equals(other.controls);
  }

  @Override
  public String toString() {
    return "ParabolaSpline{knots=" + knots + ", controls=" + controls
        + ", baselineY=" + baselineY + "}";
  }
}
