package oogasalad.model.engine;

import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * SAT-based narrow-phase collision detector implementation.
 *
 * <p>Use this class to determine whether two convex polygon objects overlap and to compute the
 * collision geometry needed by later response code.
 *
 * <p><strong>Assumptions:</strong> Input polygons are convex, their point lists are ordered
 * consistently, and upstream code supplies valid {@link GameObject} instances with polygons.
 * Degenerate or non-convex shapes can produce incorrect results.
 *
 * <p><strong>Multi-polygon objects:</strong> Each object can expose any number of convex
 * sub-polygons through {@link GameObject#getCollisionPolygons()}. This detector runs SAT on the
 * Cartesian product of sub-polygon pairs and returns the <em>deepest</em> collision among them,
 * which is sufficient for the engine's one-collision-per-pair orchestration loop. Objects with a
 * single polygon (the default) behave identically to the previous implementation.
 *
 * <p><strong>Dependencies:</strong> Relies on {@link GameObject}, {@link ConvexPolygon},
 * {@link Vector}, {@link Optional}, and the model {@link Collision} container.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionDetector detector = new CollisionDetectorImpl();
 * Optional<Collision> collision = detector.detectCollision(objectA, objectB);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class CollisionDetectorImpl implements CollisionDetector {

  @Override
  public Optional<Collision> detectCollision(GameObject objectA, GameObject objectB) {
    if (objectA == objectB) {
      return Optional.empty();
    }
    return findDeepestSubPolygonCollision(objectA, objectB);
  }

  /**
   * Iterates every pair of sub-polygons between {@code objectA} and {@code objectB} and returns
   * the collision with the largest penetration depth. Returning the deepest contact keeps the
   * one-collision-per-pair engine contract stable even when an incident object overlaps more than
   * one slab of a multi-polygon shape (for example a vehicle straddling two adjacent spline
   * slabs).
   */
  private Optional<Collision> findDeepestSubPolygonCollision(GameObject objectA,
      GameObject objectB) {
    List<ConvexPolygon> polygonsOfA = objectA.getCollisionPolygons();
    List<ConvexPolygon> polygonsOfB = objectB.getCollisionPolygons();
    if (polygonsOfA.isEmpty() || polygonsOfB.isEmpty()) {
      return Optional.empty();
    }

    Collision deepest = null;
    double deepestPenetration = 0.0;
    for (ConvexPolygon polygonA : polygonsOfA) {
      for (ConvexPolygon polygonB : polygonsOfB) {
        Optional<Collision> candidate = satBetween(objectA, objectB, polygonA, polygonB);
        if (candidate.isEmpty()) {
          continue;
        }
        double depth = candidate.get().getPenetration().magnitude();
        if (deepest == null || depth > deepestPenetration) {
          deepest = candidate.get();
          deepestPenetration = depth;
        }
      }
    }
    return Optional.ofNullable(deepest);
  }

  /**
   * Performs SAT checks on both polygon axis sets and returns empty on the first separating-axis
   * failure or the minimum-penetration collision when all checks passed.
   */
  private Optional<Collision> satBetween(GameObject objectA, GameObject objectB,
      ConvexPolygon polygonA, ConvexPolygon polygonB) {
    Optional<Collision> onAxesOfA =
        penetrationDepthOnAxesOfReference(objectA, objectB, polygonA, polygonB);
    if (onAxesOfA.isPresent()) {
      return onAxesOfA;
    }
    return penetrationDepthOnAxesOfReference(objectB, objectA, polygonB, polygonA);
  }

  private double[] projectPolygonAlongVector(ConvexPolygon polygon, Vector normal) {
    double[] projection = new double[2];
    projection[0] = Double.MAX_VALUE;
    projection[1] = -Double.MAX_VALUE;
    for (Vector point : polygon.getPoints()) {
      double shadowPoint = normal.dot(point);
      projection[0] = Math.min(shadowPoint, projection[0]);
      projection[1] = Math.max(shadowPoint, projection[1]);
    }
    return projection;
  }

  /**
   * Detects collisions using the SAT along the edges of {@code referencePolygon} and tracks the
   * smallest penetration vector across those edges.
   */
  private Optional<Collision> penetrationDepthOnAxesOfReference(GameObject referenceObject,
      GameObject incidentObject, ConvexPolygon referencePolygon, ConvexPolygon incidentPolygon) {

    Collision collision = new Collision(referenceObject, incidentObject);
    double minPenetrationDepth = Double.MAX_VALUE;
    Vector penetration = null;
    Vector[] minReferenceEdge = new Vector[2];
    for (int i = 0; i < referencePolygon.getPoints().size() - 1; i++) {
      Vector point1 = referencePolygon.getPoints().get(i);
      Vector point2 = referencePolygon.getPoints().get(i + 1);
      Vector tangent = point2.subtract(point1).normalize();
      Vector normal = tangent.rotateRight();

      double[] projectionA = projectPolygonAlongVector(referencePolygon, normal);
      double[] projectionB = projectPolygonAlongVector(incidentPolygon, normal);
      boolean shadowsOverlap = projectionA[1] <= projectionB[0] || projectionB[1] <= projectionA[0];
      if (shadowsOverlap) {
        return Optional.empty();
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]);
      if (currentPenetrationDepth < minPenetrationDepth) {
        penetration = normal.multiply(currentPenetrationDepth);
        minPenetrationDepth = currentPenetrationDepth;
        minReferenceEdge[0] = point1;
        minReferenceEdge[1] = point2;
      }
    }

    if (penetration == null) {
      return Optional.empty();
    }

    collision.setPenetration(penetration);
    collision.setReferenceEdge(minReferenceEdge);
    collision.setIncidentEdge(findIncidentEdge(incidentPolygon, penetration));
    return Optional.of(collision);
  }

  /**
   * Selects the incident edge whose outward normal best matches the collision normal.
   */
  private Vector[] findIncidentEdge(ConvexPolygon incidentPolygon, Vector penetrationNormal) {
    Vector[] incidentEdge = new Vector[2];
    double minimumDotProduct = Double.MAX_VALUE;

    List<Vector> points = incidentPolygon.getPoints();
    for (int i = 0; i < points.size() - 1; i++) {
      Vector incidentNormal = points.get(i + 1).subtract(points.get(i)).normalize().rotateRight();
      double dotWithIncident = penetrationNormal.dot(incidentNormal);
      if (dotWithIncident < minimumDotProduct) {
        minimumDotProduct = dotWithIncident;
        incidentEdge[0] = points.get(i);
        incidentEdge[1] = points.get(i + 1);
      }
    }
    return incidentEdge;
  }
}
