package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;

/**
 * Invisible static collider that wraps one tessellated slab of a {@link TerrainSpline}.
 *
 * <p>The visible spline lives in the world as a single {@link TerrainSpline} (rendered via a
 * JavaFX path). Physics runs against per-slab convex polygons because the engine's collision
 * detector resolves only one polygon per object. A {@code TerrainSpline} therefore spawns one of
 * these per slab so a vehicle driving along the curve meets a collidable surface across the whole
 * arc, not just the first slab.
 */
public class SplineSlabCollider extends StaticComponent {

  public SplineSlabCollider(ConvexPolygon slab, double friction, double restitution) {
    super(firstX(slab), firstY(slab), 0.0, slab, DefaultTag.TERRAIN.value(), friction, restitution);
  }

  private static double firstX(ConvexPolygon slab) {
    return slab.points().isEmpty() ? 0.0 : slab.points().getFirst().x();
  }

  private static double firstY(ConvexPolygon slab) {
    return slab.points().isEmpty() ? 0.0 : slab.points().getFirst().y();
  }
}
