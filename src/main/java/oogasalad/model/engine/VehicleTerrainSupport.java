package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;

/**
 * Prevents the assembled vehicle (vehicle-tagged parts + pigs) from resting below the top surface
 * of terrain blocks under its horizontal footprint. Gravity + soft joint solving can otherwise push
 * the lowest pieces visually into the ground; this applies a rigid vertical correction to the
 * whole assembly.
 */
public final class VehicleTerrainSupport {

  private static final double SURFACE_EPSILON = 0.75;

  private VehicleTerrainSupport() {}

  /**
   * If the lowest point of the vehicle assembly is below the supporting terrain top (under the
   * assembly's x-span), translates every assembly member by the same delta so the lowest point
   * sits on or just above that surface.
   */
  public static void keepVehicleAssemblyAboveTerrainSurface(World world) {
    String vehicleTag = DefaultTag.VEHICLE.value();
    String pigsTag = DefaultTag.PIGS.value();

    List<TerrainBlock> terrains = new ArrayList<>();
    List<GameComponent> assembly = new ArrayList<>();
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof TerrainBlock tb) {
        terrains.add(tb);
      } else if (vehicleTag.equals(c.getTag()) || pigsTag.equals(c.getTag())) {
        assembly.add(c);
      }
    }
    if (assembly.isEmpty() || terrains.isEmpty()) {
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

    double supportTopY = supportSurfaceYUnderSpan(terrains, minX, maxX);
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
   * With +Y downward, each terrain block's walkable top is the minimum y of its polygon. Under
   * the vehicle x-span we take the deepest (largest y) such top among overlapping blocks so a wide
   * vehicle clears the highest step it spans.
   */
  private static double supportSurfaceYUnderSpan(List<TerrainBlock> terrains, double minX,
      double maxX) {
    double deepestTop = Double.NEGATIVE_INFINITY;
    for (TerrainBlock tb : terrains) {
      double left = tb.getX();
      double right = left + tb.getWidth();
      if (right < minX || left > maxX) {
        continue;
      }
      tb.updatePolygon();
      var poly = tb.getPolygon();
      if (poly == null) {
        continue;
      }
      double topY = Double.POSITIVE_INFINITY;
      for (var p : poly.getPoints()) {
        topY = Math.min(topY, p.y());
      }
      deepestTop = Math.max(deepestTop, topY);
    }
    return deepestTop;
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
