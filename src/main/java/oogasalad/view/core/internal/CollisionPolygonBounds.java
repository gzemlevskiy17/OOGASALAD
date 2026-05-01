package oogasalad.view.core.internal;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Axis-aligned bounds from simulated collision polygons, for viewport overlap tests.
 *
 * @author Iurii Beliaev
 */
final class CollisionPolygonBounds {

  private CollisionPolygonBounds() {
  }

  /**
   * Axis-aligned bounding box enclosing every vertex of every collision polygon (world space).
   * Defined is false when there are no finite vertices.
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