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
  //TODO: test & refactor
  //TODO: tolerances
  //TODO: update javadoc once complete

  /**
   * Returns a collision if detected by {@code findSatCollision}.
   *
   * @param objectA first object in the pair
   * @param objectB second object in the pair
   * @return an {@link Optional} containing the detected collision, or {@link Optional#empty()} when
   * the shapes do not overlap
   */
  @Override
  public Optional<Collision> detectCollision(GameObject objectA, GameObject objectB) {
    return findSatCollision(objectA, objectB);
  }

  /**
   * Projects all polygon vertices onto the given axis and returns [min, max] values of the
   * endpoints of the shadow of the shape along the normal. The shadow is cast in the tangent
   * direction, so the endpoints form a line segment parallel to the normal.
   *
   * <p>The returned array contains the minimum and maximum scalar projection of the polygon along
   * {@code normal}.
   *
   * @param polygon polygon to project
   * @param normal  axis used for the projection
   * @return a two-element array containing the minimum and maximum projection values
   */
  private double[] projectPolygonAlongVector(ConvexPolygon polygon, Vector normal) {
    double[] projection = new double[2];
    projection[0] = Double.MAX_VALUE;
    projection[1] = -Double.MAX_VALUE;
    for (Vector point : polygon.getPoints()) { //Check all points to find those that cast the biggest shadow.
      double shadowPoint = normal.dot(point);
      projection[0] = Math.min(shadowPoint, projection[0]);
      projection[1] = Math.max(shadowPoint, projection[1]);
    }

    return projection;
  }

  /**
   * Performs SAT checks on both polygon axis sets and returns empty on the first separating-axis
   * failure or the  minimum-penetration collision when all checks passed.
   *
   * <p>The method first checks the axes derived from {@code objectA}; if no separating axis is
   * found there, it repeats the test with the objects swapped so both edge sets are considered.
   *
   * @param objectA first object to test
   * @param objectB second object to test
   * @return a collision record when all axes overlap, otherwise {@link Optional#empty()}
   */
  private Optional<Collision> findSatCollision(GameObject objectA, GameObject objectB) {
    if (objectA == null || objectB == null || objectA == objectB) {
      return Optional.empty();
    }

    Optional<Collision> collisionOnAxesOfA = penetrationDepthOnAxesOfA(objectA, objectB);
    if (collisionOnAxesOfA.isPresent()) {
      return collisionOnAxesOfA;
    }
    return penetrationDepthOnAxesOfA(objectB, objectA);
  }

  /**
   * Detects collisions using the SAT along the edges of {@code referenceObject} and tracks the
   * smallest penetration vector across those edges. Tests the separating axes defined by the edges
   * of the reference polygon.
   *
   * <p>The smallest penetration axis becomes the collision normal when no separating axis is
   * found.
   *
   * @param referenceObject object supplying the axes to test
   * @param incidentObject  object projected against those axes
   * @return a collision record when projections overlap on every tested axis, otherwise empty
   */
  private Optional<Collision> penetrationDepthOnAxesOfA(GameObject referenceObject,
      GameObject incidentObject) {
    //TODO: rename method (setters in the end)
    ConvexPolygon referencePolygon = referenceObject.getPolygon();
    ConvexPolygon incidentPolygon = incidentObject.getPolygon();

    Collision collision = new Collision(referenceObject, incidentObject);
    double minPenetrationDepth = Double.MAX_VALUE;
    Vector penetration = null;
    Vector[] minReferenceEdge = new Vector[2];
    for (int i = 0; i < referencePolygon.getPoints().size() - 1;
        i++) { // Looping over edges clockwise
      Vector point1 = referencePolygon.getPoints().get(i);
      Vector point2 = referencePolygon.getPoints().get(i + 1);
      Vector tangent = point2.subtract(point1).normalize();
      Vector inwardUnitNormal = tangent.rotateRight(); //Find the inward normal along which to compute the penetration depth

      double[] projectionA = projectPolygonAlongVector(referencePolygon, inwardUnitNormal);
      double[] projectionB = projectPolygonAlongVector(incidentPolygon, inwardUnitNormal);
      boolean noOverlap =
          projectionA[1] <= projectionB[0] || projectionB[1] <= projectionA[0]; //TODO: clean logic
      if (!noOverlap) { //if the shadows don't overlap in at least one axis, there is no collision by the SAT
        return Optional.empty();
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]);
      if (currentPenetrationDepth < minPenetrationDepth) {
        penetration = inwardUnitNormal.multiply(currentPenetrationDepth);
        minPenetrationDepth = currentPenetrationDepth;
        minReferenceEdge[0] = point1;
        minReferenceEdge[1] = point2;
      }
    }
    collision.setPenetration(penetration);
    collision.setReferenceEdge(minReferenceEdge);
    collision.setIncidentEdge(findIncidentEdge(incidentPolygon, penetration));
    return Optional.of(collision);
  }

  /**
   * Selects the incident edge whose outward normal best matches the collision normal.
   *
   * <p>Assumes clockwise polygon point order and a non-null penetration normal.
   *
   * @param incidentPolygon   polygon supplying the candidate incident edges
   * @param penetrationNormal collision normal used to choose the most opposing edge
   * @return the incident edge that should be clipped against the reference edge
   */
  private Vector[] findIncidentEdge(ConvexPolygon incidentPolygon, Vector penetrationNormal) {
    Vector[] incidentEdge = new Vector[2];
    double minimumDotProduct = Double.MAX_VALUE;

    List<Vector> points = incidentPolygon.getPoints();
    for (int i = 0; i < points.size() - 1; i++) {
      Vector incidentNormal = points.get(i + 1).subtract(points.get(i)).normalize().rotateLeft();
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
