package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;

/**
 * Prevents the assembled vehicle (vehicle-tagged parts + pigs) from resting below the top surface
 * of terrain under its horizontal footprint. Gravity + soft joint solving can otherwise push the
 * lowest pieces visually into the ground; this applies a rigid vertical correction to the whole
 * assembly.
 *
 * <p>Both {@link TerrainBlock}s and the per-slab polygons of a {@link TerrainSpline} are treated
 * as candidate "ground patches" — each patch contributes a top Y under the vehicle's x-span, and
 * the correction snaps the lowest assembly vertex to the deepest overlapping top. This lets
 * spline-based levels benefit from the same rigid safety net as block-based ones, without
 * requiring the spline to know anything about vehicles.
 */
public final class VehicleTerrainSupport {

  private static final double SURFACE_EPSILON = 0.75;

  private VehicleTerrainSupport() {}

  /**
   * Axis-aligned "ground patch" record used internally to abstract over terrain sources: a
   * {@link TerrainBlock}'s bounding box, or a single convex slab from a {@link TerrainSpline}.
   */
  private record GroundPatch(double leftX, double rightX, double topY) {}

  /**
   * If the lowest point of the vehicle assembly is below the supporting terrain top (under the
   * assembly's x-span), translates every assembly member by the same delta so the lowest point
   * sits on or just above that surface.
   */
  public static void keepVehicleAssemblyAboveTerrainSurface(World world) {
    String vehicleTag = DefaultTag.VEHICLE.value();
    String pigsTag = DefaultTag.PIGS.value();

    List<GroundPatch> patches = new ArrayList<>();
    List<GameComponent> assembly = new ArrayList<>();
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof TerrainBlock tb) {
        addBlockPatch(patches, tb);
      } else if (c instanceof TerrainSpline ts) {
        addSplinePatches(patches, ts);
      } else if (vehicleTag.equals(c.getTag()) || pigsTag.equals(c.getTag())) {
        assembly.add(c);
      }
    }
    if (assembly.isEmpty() || patches.isEmpty()) {
      return;
    }

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double lowestY = Double.NEGATIVE_INFINITY;
    for (GameComponent c : assembly) {
      c.updatePolygon();
      var poly = c.getPolygon();
      if (poly == null) {
        continue;
      }
      for (var p : poly.getPoints()) {
        minX = Math.min(minX, p.x());
        maxX = Math.max(maxX, p.x());
        lowestY = Math.max(lowestY, p.y());
      }
    }
    if (minX == Double.POSITIVE_INFINITY || lowestY == Double.NEGATIVE_INFINITY) {
      return;
    }

    double supportTopY = supportSurfaceYUnderSpan(patches, minX, maxX);
    if (supportTopY == Double.NEGATIVE_INFINITY) {
      return;
    }

    if (lowestY <= supportTopY + SURFACE_EPSILON) {
      return;
    }
    double dy = supportTopY + SURFACE_EPSILON - lowestY;
    translateAssembly(assembly, dy);
  }

  /**
   * With +Y downward, each ground patch's walkable top is stored explicitly; under the vehicle
   * x-span we take the deepest (largest y) such top among overlapping patches so a wide vehicle
   * clears the highest step it spans.
   */
  private static double supportSurfaceYUnderSpan(List<GroundPatch> patches, double minX,
      double maxX) {
    double deepestTop = Double.NEGATIVE_INFINITY;
    for (GroundPatch patch : patches) {
      if (patch.rightX() < minX || patch.leftX() > maxX) {
        continue;
      }
      deepestTop = Math.max(deepestTop, patch.topY());
    }
    return deepestTop;
  }

  private static void addBlockPatch(List<GroundPatch> patches, TerrainBlock tb) {
    tb.updatePolygon();
    var poly = tb.getPolygon();
    if (poly == null) {
      return;
    }
    double topY = Double.POSITIVE_INFINITY;
    for (var p : poly.getPoints()) {
      topY = Math.min(topY, p.y());
    }
    patches.add(new GroundPatch(tb.getX(), tb.getX() + tb.getWidth(), topY));
  }

  /**
   * A spline surfaces as one patch per tessellated slab. Each slab's top edge is the walkable
   * surface at that X-range; treating slabs independently lets the span lookup pick the correct
   * local ground height even when the curve varies significantly across the vehicle's footprint.
   */
  private static void addSplinePatches(List<GroundPatch> patches, TerrainSpline ts) {
    for (ConvexPolygon slab : ts.getCollisionPolygons()) {
      double leftX = Double.POSITIVE_INFINITY;
      double rightX = Double.NEGATIVE_INFINITY;
      double topY = Double.POSITIVE_INFINITY;
      for (var p : slab.getPoints()) {
        leftX = Math.min(leftX, p.x());
        rightX = Math.max(rightX, p.x());
        topY = Math.min(topY, p.y());
      }
      if (leftX == Double.POSITIVE_INFINITY) {
        continue;
      }
      patches.add(new GroundPatch(leftX, rightX, topY));
    }
  }

  private static void translateAssembly(List<GameComponent> assembly, double dy) {
    Vector delta = new Vector(0, dy);
    for (GameComponent c : assembly) {
      Vector pos = c.getState().getLinearPosition();
      c.getState().setLinearPosition(pos.add(delta));
      c.updatePolygon();
    }
  }
}
