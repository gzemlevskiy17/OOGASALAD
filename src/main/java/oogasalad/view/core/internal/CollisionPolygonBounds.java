package oogasalad.view.core.internal;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Axis-aligned bounds derived from collision polygons for camera visibility tests.
 *
 * @author Iurii Beliaev
 */
final class CollisionPolygonBounds {

  private CollisionPolygonBounds() {
  }

  /**
   * Union bounding box of every collision vertex in world coordinates, or defined false when empty or non-finite.
   */
  static UnionAabb unionAabb(GameObject object) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    boolean any = false;

    for (ConvexPolygon polygon : object.getCollisionPolygons()) {
      if (polygon == null || polygon.points() == null) {
        continue;
      }
      for (Vector point : polygon.points()) {
        if (point == null) {
          continue;
        }
        any = true;
        minX = Math.min(minX, point.x());
        minY = Math.min(minY, point.y());
        maxX = Math.max(maxX, point.x());
        maxY = Math.max(maxY, point.y());
      }
    }
    if (!any || !finiteBounds(minX, minY, maxX, maxY)) {
      return UnionAabb.none();
    }
    return UnionAabb.of(minX, minY, maxX, maxY);
  }

  /** True when two axis-aligned rectangles overlap with inclusive edges. */
  static boolean rectsOverlap(double aMinX, double aMinY, double aMaxX, double aMaxY,
      double bMinX, double bMinY, double bMaxX, double bMaxY) {
    return aMaxX >= bMinX && aMinX <= bMaxX && aMaxY >= bMinY && aMinY <= bMaxY;
  }

  private static boolean finiteBounds(double minX, double minY, double maxX, double maxY) {
    return Double.isFinite(minX) && Double.isFinite(minY)
        && Double.isFinite(maxX) && Double.isFinite(maxY)
        && minX <= maxX && minY <= maxY;
  }

  record UnionAabb(boolean defined, double minX, double minY, double maxX, double maxY) {

    static UnionAabb none() {
      return new UnionAabb(false, 0, 0, 0, 0);
    }

    static UnionAabb of(double minX, double minY, double maxX, double maxY) {
      return new UnionAabb(true, minX, minY, maxX, maxY);
    }
  }
}
