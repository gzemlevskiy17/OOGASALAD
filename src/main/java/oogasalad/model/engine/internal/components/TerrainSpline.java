package oogasalad.model.engine.internal.components;

import java.util.List;
import java.util.Objects;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.model.engine.internal.spline.SplineTessellator;

/**
 * Static terrain whose collision surface follows a chain of parabola (quadratic Bézier) segments.
 *
 * <p>Each spline is authored as a small list of knots with a baseline, then tessellated into a
 * strip of convex slabs at construction time. {@link #getCollisionPolygons()} exposes those slabs
 * to the physics engine directly, so a spline is modelled as one {@link GameComponent} with many
 * sub-polygons rather than as many derived objects — keeping save/load, world iteration, and
 * object ownership simple while letting the engine collide against every slab.
 *
 * <p>{@link #getPolygon()} returns the first slab so single-polygon callers (e.g. tests, debug
 * tooling) still work; the broad-phase AABB and narrow-phase SAT both go through
 * {@link #getCollisionPolygons()}.
 *
 * <p>The component's position ({@link #x()}, {@link #y()}) is the axis-aligned bounding-box
 * top-left corner of the spline curve. {@link #getWidth()} and {@link #getHeight()} describe the
 * bounding box down to the baseline — these are what the gameplay factory uses to compute world
 * bounds, and what the authoring canvas uses for the fallback rectangle drawing path.
 *
 * <p>Note: the physics slabs returned by {@link #getCollisionPolygons()} only extend a fixed
 * {@link SplineTessellator#DEFAULT_SLAB_THICKNESS} below the curve, even though the visual fill
 * drawn by the renderer reaches all the way down to the baseline. Shallow slabs keep SAT stable
 * (see {@link SplineTessellator} for details); the baseline is a purely cosmetic floor.
 *
 * @author Bad Piggies team
 */
public class TerrainSpline extends TerrainComponent {

  private static final String DEFAULT_COLOR = "#4A2C0F";
  private static final String DEFAULT_SPRITE = "";
  private static final double DEFAULT_RESTITUTION = 0.5;

  private final ParabolaSpline spline;
  private final List<ConvexPolygon> slabs;
  private final double width;
  private final double height;
  private final String color;
  private final String sprite;
  private final boolean tiled;

  /**
   * @param spline   parabola spline describing the curve and its baseline
   * @param sprite   texture path or identifier; may be empty if no sprite is desired
   * @param tiled    true to tile the sprite, false to stretch
   * @param friction surface friction coefficient (non-negative)
   * @param color    fallback CSS color when the sprite cannot be loaded
   */
  public TerrainSpline(ParabolaSpline spline, String sprite, boolean tiled, double friction,
      String color) {
    super(boundingMinX(Objects.requireNonNull(spline, "spline")), boundingMinY(spline), 0.0,
        firstSlabOrPlaceholder(spline), "", friction, DEFAULT_RESTITUTION);
    this.spline = spline;
    this.slabs = SplineTessellator.tessellate(spline);
    this.width = boundingMaxX(spline) - boundingMinX(spline);
    this.height = spline.baselineY() - boundingMinY(spline);
    this.sprite = sprite == null ? DEFAULT_SPRITE : sprite;
    this.tiled = tiled;
    this.color = color == null ? DEFAULT_COLOR : color;

    // Once the slab list exists, expose the first slab as the single-polygon view so getPolygon()
    // remains consistent with the physics surface (the constructor's super() call only had access
    // to a freshly tessellated list captured by firstSlabOrPlaceholder).
    if (!slabs.isEmpty()) {
      setPolygon(slabs.get(0));
    }
  }

  public TerrainSpline(ParabolaSpline spline, double friction) {
    this(spline, DEFAULT_SPRITE, false, friction, DEFAULT_COLOR);
  }

  public ParabolaSpline getSpline() {
    return spline;
  }

  private static ConvexPolygon firstSlabOrPlaceholder(ParabolaSpline spline) {
    List<ConvexPolygon> tessellated = SplineTessellator.tessellate(spline);
    if (!tessellated.isEmpty()) {
      return tessellated.get(0);
    }
    return ConvexPolygon.axisAlignedBox(boundingMinX(spline), boundingMinY(spline),
        Math.max(1.0, boundingMaxX(spline) - boundingMinX(spline)),
        Math.max(1.0, spline.baselineY() - boundingMinY(spline)));
  }

  @Override
  public double getWidth() {
    return width;
  }

  @Override
  public double getHeight() {
    return height;
  }

  @Override
  public String getSprite() {
    return sprite;
  }

  @Override
  public boolean isTiled() {
    return tiled;
  }

  @Override
  public List<ConvexPolygon> getCollisionPolygons() {
    return slabs;
  }

  @Override
  public String getColor() {
    return color;
  }

  private static double boundingMinX(ParabolaSpline spline) {
    double minX = Double.POSITIVE_INFINITY;
    for (Vector k : spline.knots()) {
      minX = Math.min(minX, k.x());
    }
    return minX;
  }

  private static double boundingMaxX(ParabolaSpline spline) {
    double maxX = Double.NEGATIVE_INFINITY;
    for (Vector k : spline.knots()) {
      maxX = Math.max(maxX, k.x());
    }
    return maxX;
  }

  private static double boundingMinY(ParabolaSpline spline) {
    double minY = Double.POSITIVE_INFINITY;
    for (Vector k : spline.knots()) {
      minY = Math.min(minY, k.y());
    }
    return minY;
  }
}
