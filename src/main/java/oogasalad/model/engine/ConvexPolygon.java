package oogasalad.model.engine;

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
public class ConvexPolygon {
  //TODO: define as interface?

  private List<Vector> points;

  public ConvexPolygon(List<Vector> points) {
    this.points = points;
  }

  public List<Vector> getPoints() { //in CW order
    return Collections.unmodifiableList(points);
  }
}
