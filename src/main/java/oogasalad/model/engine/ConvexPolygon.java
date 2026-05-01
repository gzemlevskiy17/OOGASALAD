package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import oogasalad.model.engine.api.Vector;

/**
 * Represents a convex polygon used for collision detection.
 *
 * <p>Use this container to pass ordered polygon vertices into SAT projection and clipping logic.
 *
 * <p><strong>Assumptions:</strong> Vertex order is consistent (current code expects clockwise) and
 * polygon is convex. Non-convex input can produce incorrect collision results.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link Vector} for vertex coordinates and
 * Java {@link List} for storage.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * ConvexPolygon box = new ConvexPolygon(List.of(
 *     new Vector(0, 0), new Vector(2, 0), new Vector(2, 1), new Vector(0, 1), new Vector(0, 0)));
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public record ConvexPolygon(List<Vector> points) {

  public static ConvexPolygon axisAlignedBox(double x, double y, double w, double h) {
    return new ConvexPolygon(List.of(
        new Vector(x, y),
        new Vector(x + w, y),
        new Vector(x + w, y + h),
        new Vector(x, y + h),
        new Vector(x, y)
    ));
  }

  /**
   * Creates a box collider centered at ({@code centerX}, {@code centerY}) rotated by
   * {@code radians} around its center.
   */
  public static ConvexPolygon rotatedBox(double centerX, double centerY, double w, double h,
      double radians) {
    Vector center = new Vector(centerX, centerY);
    Vector bottomLeft = new Vector(-w / 2.0, -h / 2.0).rotate(radians).add(center);
    Vector bottomRight = new Vector(w / 2.0, -h / 2.0).rotate(radians).add(center);
    Vector topRight = new Vector(w / 2.0, h / 2.0).rotate(radians).add(center);
    Vector topLeft = new Vector(-w / 2.0, h / 2.0).rotate(radians).add(center);
    return new ConvexPolygon(List.of(bottomLeft, bottomRight, topRight, topLeft, bottomLeft));
  }

  /**
   * Creates a closed regular polygon centered at ({@code centerX}, {@code centerY}).
   */
  public static ConvexPolygon regularPolygon(double centerX, double centerY, double radius,
      int sides, double radians) {
    if (sides < 3) {
      throw new IllegalArgumentException("regular polygon must have at least 3 sides");
    }
    Vector center = new Vector(centerX, centerY);
    List<Vector> vertices = new ArrayList<>();
    for (int i = 0; i < sides; i++) {
      double angle = radians + (2.0 * Math.PI * i / sides);
      vertices.add(new Vector(radius * Math.cos(angle), radius * Math.sin(angle)).add(center));
    }
    vertices.add(vertices.getFirst());
    return new ConvexPolygon(vertices);
  }

  @Override
  public List<Vector> points() { //in CW order
    return Collections.unmodifiableList(points);
  }
}
