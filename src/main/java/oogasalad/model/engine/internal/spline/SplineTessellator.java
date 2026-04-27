package oogasalad.model.engine.internal.spline;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;

/**
 * Converts a {@link ParabolaSpline} into a strip of convex quadrilateral slabs suitable for the
 * existing SAT-based collision engine.
 *
 * <p>Each Bézier segment becomes one quadrilateral whose top edge is the chord between its two
 * knots and whose bottom edge sits {@code slabThickness} world pixels below those endpoints.
 * Curve fidelity is therefore controlled by knot density at authoring time, not by an extra
 * sampling resolution. The polygons are emitted in clockwise order (matching the project
 * convention used by {@link ConvexPolygon#axisAlignedBox}).
 *
 * <p>Why a fixed slab thickness instead of filling all the way to the spline baseline: the SAT
 * collision detector returns the axis of <em>minimum</em> penetration, and the engine's per-pair
 * contract resolves only the deepest sub-polygon overlap each frame. When slabs extend hundreds
 * of pixels below the curve, a vehicle that penetrates past the curve can end up closer to the
 * slab's vertical side edges than to its top edge, at which point SAT pushes the vehicle
 * sideways ("ghost collisions") instead of back up. Shallow slabs keep the top edge the
 * dominant axis and prevent vehicles from popping out the bottom when the per-frame penetration
 * grows large. The visual ground fill is still drawn all the way down to {@link
 * ParabolaSpline#baselineY()} by the renderer — that happens outside of physics.
 *
 * <p>Why quadrilaterals instead of triangles: the engine's SAT implementation uses edge-based
 * projection axes, and triangulating each slab would double the number of polygons for no
 * collision-quality gain. A quad with monotonically-increasing-X top edge is always convex.
 *
 * <p>Degenerate sub-intervals (where consecutive sampled top points coincide in X) are skipped so
 * the emitted polygons always have nonzero area and well-defined edge normals.
 *
 * @author Bad Piggies team
 */
public final class SplineTessellator {

  private static final double MIN_HORIZONTAL_SPAN = 1e-6;
  /**
   * Default physics slab thickness in world pixels. Large enough to survive per-frame fall
   * velocities at typical engine timesteps (≥ ~3 seconds of freefall under default gravity),
   * small enough to stay much shorter than slab width so SAT always chooses the top edge as the
   * minimum-penetration axis during landing.
   */
  public static final double DEFAULT_SLAB_THICKNESS = 200.0;

  private SplineTessellator() {
    // utility class
  }

  /**
   * Returns the list of convex-polygon slabs that physically represent the given spline, using
   * {@link #DEFAULT_SLAB_THICKNESS} for the physics layer.
   *
   * @param spline non-null spline
   * @return ordered list of convex polygons covering a thin strip beneath the curve
   */
  public static List<ConvexPolygon> tessellate(ParabolaSpline spline) {
    return tessellate(spline, DEFAULT_SLAB_THICKNESS);
  }

  /**
   * Returns the list of convex-polygon slabs that physically represent the given spline with a
   * caller-chosen slab thickness.
   *
   * @param spline        non-null spline
   * @param slabThickness height of each physics slab below the curve, in world pixels;
   *                      must be strictly positive
   * @return ordered list of convex polygons covering a strip of the given thickness beneath the
   *     curve
   */
  public static List<ConvexPolygon> tessellate(ParabolaSpline spline, double slabThickness) {
    if (slabThickness <= 0) {
      throw new IllegalArgumentException(
          "slabThickness must be strictly positive; got " + slabThickness);
    }
    List<ConvexPolygon> slabs = new ArrayList<>();
    for (int segment = 0; segment < spline.segmentCount(); segment++) {
      Vector left = spline.knots().get(segment);
      Vector right = spline.knots().get(segment + 1);
      addSlabIfNonDegenerate(slabs, left, right, slabThickness);
    }
    return slabs;
  }

  private static void addSlabIfNonDegenerate(List<ConvexPolygon> slabs, Vector leftTop,
      Vector rightTop, double slabThickness) {
    if (Math.abs(rightTop.x() - leftTop.x()) < MIN_HORIZONTAL_SPAN) {
      return;
    }
    if (rightTop.x() < leftTop.x()) {
      // Keep winding clockwise even if the curve folds back in X: swap to restore left-to-right
      // ordering on the top edge.
      Vector swap = leftTop;
      leftTop = rightTop;
      rightTop = swap;
    }
    // Each slab's bottom edge is a constant slabThickness below its own top corners, which keeps
    // the slab shallow and rectangular-ish even when the curve has large Y variation between
    // adjacent samples.
    List<Vector> points = List.of(
        leftTop,
        rightTop,
        new Vector(rightTop.x(), rightTop.y() + slabThickness),
        new Vector(leftTop.x(), leftTop.y() + slabThickness),
        leftTop
    );
    slabs.add(new ConvexPolygon(points));
  }
}
