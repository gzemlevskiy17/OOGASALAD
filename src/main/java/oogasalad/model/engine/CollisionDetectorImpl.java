package oogasalad.model.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Optional<Collision> collision = findSatCollision(objectA, objectB);
    collision.ifPresent(this::addManifold);
    return collision;
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
    Optional<Collision> collisionOnAxesOfA = buildCollision(objectA, objectB);
    if (collisionOnAxesOfA.isPresent()) {
      return collisionOnAxesOfA;
    }
    return buildCollision(objectB, objectA);
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
  private Optional<Collision> buildCollision(GameObject referenceObject,
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
      Vector outwardUnitNormal = tangent.rotateRight(); //Find the inward normal along which to compute the penetration depth
      //TODO: TEST

      double[] projectionA = projectPolygonAlongVector(referencePolygon, outwardUnitNormal);
      double[] projectionB = projectPolygonAlongVector(incidentPolygon, outwardUnitNormal);
      boolean noOverlap =
          projectionA[1] <= projectionB[0]
              || projectionB[1] <= projectionA[0]; //TODO: make easier to understand
      if (noOverlap) { //if the shadows don't overlap in at least one axis, there is no collision by the SAT
        return Optional.empty();
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]);
      if (currentPenetrationDepth < minPenetrationDepth) {
        penetration = outwardUnitNormal.multiply(currentPenetrationDepth);
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
   *
   * <p>Assumes clockwise polygon point order and a non-null penetration normal.
   *
   * @param incidentPolygon   polygon supplying the candidate incident edges
   * @param penetrationNormal collision normal used to choose the most opposing edge
   * @return the incident edge that will be clipped against the reference edge
   */
  private Vector[] findIncidentEdge(ConvexPolygon incidentPolygon, Vector penetrationNormal) {
    Vector[] incidentEdge = new Vector[2];
    double minimumDotProduct = Double.MAX_VALUE;

    List<Vector> points = incidentPolygon.getPoints();
    for (int i = 0; i < points.size() - 1; i++) {
      Vector incidentNormal = points.get(i + 1).subtract(points.get(i)).normalize()
          .rotateRight(); // maybe rotateLeft
      double dotWithIncident = penetrationNormal.dot(incidentNormal);
      if (dotWithIncident < minimumDotProduct) {
        minimumDotProduct = dotWithIncident;
        incidentEdge[0] = points.get(i);
        incidentEdge[1] = points.get(i + 1);
      }
    }
    return incidentEdge;
  }

  /**
   * Clips the incident edge against the reference edge and keeps the contact points that lie inside
   * the reference face.
   *
   * <p>Assumes at least one endpoint of the incident edge is inside the reference polygon and that
   * the reference edge preserves clockwise ordering.
   *
   * @param clippedIncidentEdge incident edge after side clipping
   * @param referenceEdge       reference edge used to compute contact depth
   * @return contact points that lie behind the reference edge
   */
  private Map<Vector, Double> faceClip(Vector[] clippedIncidentEdge, Vector[] referenceEdge) {
    Vector referenceOutwardNormal = referenceEdge[1].subtract(referenceEdge[0]).normalize()
        .rotateRight();
    Map<Vector, Double> manifoldPoints = new HashMap<>();
    double distanceFromLeftPlane = distanceFromPlane(clippedIncidentEdge[0], referenceEdge[0],
        referenceOutwardNormal);
    if (distanceFromLeftPlane < 0) {
      manifoldPoints.put(clippedIncidentEdge[0], -distanceFromLeftPlane);
    }

    double distanceFromRightPlane = distanceFromPlane(clippedIncidentEdge[1], referenceEdge[0],
        referenceOutwardNormal);
    if (distanceFromRightPlane < 0) {
      manifoldPoints.put(clippedIncidentEdge[1], -distanceFromRightPlane);
    }

    return manifoldPoints;
  }

  /**
   * Computes the signed distance of a point from a plane defined by a point and normal.
   *
   * @param point       point being tested
   * @param planePoint  point on the plane
   * @param planeNormal outward normal of the plane
   * @return signed distance from the plane
   */
  private double distanceFromPlane(Vector point, Vector planePoint, Vector planeNormal) {
    return point.subtract(planePoint).dot(planeNormal);
  }


  /**
   * Builds the contact manifold for the detected collision.
   *
   * <p>The method clips the incident edge against the reference edge before keeping only points
   * that lie inside the reference face.
   * <p>
   * hand-tested: clippedIncidentEdge = [(0, 1), (8, 9)] referenceEdge = [(0, 5), (8, 5)]
   * referenceOutwardNormal = (0, 1): only adds (0, 1) to manifold, not (8, 9), which is not in the
   * reference polygon.
   *
   * @param collision detected collision pair
   * @return contact points suitable for impulse resolution
   */
  private void addManifold(Collision collision) {
    Vector[] incidentEdge = collision.getIncidentEdge();
    Vector[] referenceEdge = collision.getReferenceEdge();
    Vector[] clippedIncidentEdge = clipToReferenceEdge(incidentEdge, referenceEdge);
    collision.setManifold(faceClip(clippedIncidentEdge, referenceEdge));
  }


  /**
   * Clips the incident edge against the side planes of the reference edge.
   *
   * <p>The result preserves the portion of the incident edge that lies between the reference edge
   * endpoints.
   *
   * @param incidentEdge  edge from the incident polygon
   * @param referenceEdge edge from the reference polygon
   * @return the incident edge clipped to the reference edge extents
   */
  private Vector[] clipToReferenceEdge(Vector[] incidentEdge, Vector[] referenceEdge) {
    // 4 situations: incident edge has both, either, or no endpoints outside of reference edge.
    // Assuming that the incident edge will intersect the reference edge, and at least some part of
    // the incident edge is within the reference edge endpoints
    Vector sideANormal = referenceEdge[0].subtract(referenceEdge[1]);
    Vector sideBNormal = referenceEdge[1].subtract(referenceEdge[0]);

    //find where the endpoints of the incident edge are relative to the planes
    Vector unclippedSideAEndpoint = null;
    Vector unclippedSideBEndpoint = null;
    if (incidentEdge[0].subtract(referenceEdge[0]).dot(sideANormal) > incidentEdge[1].subtract(
        referenceEdge[0]).dot(
        sideANormal)) { //the more positive dot product with the outward A normal, the farthest that point is in the outward direction
      unclippedSideAEndpoint = incidentEdge[0];
      unclippedSideBEndpoint = incidentEdge[1];
    } else {
      unclippedSideAEndpoint = incidentEdge[1];
      unclippedSideBEndpoint = incidentEdge[0];
    }
    Vector clippedIncidentAEndpoint = unclippedSideAEndpoint;
    if (distanceFromPlane(unclippedSideAEndpoint, referenceEdge[0], sideANormal) > 0) {
      //compute the point of intersection of the incident edge and the side A plane
      clippedIncidentAEndpoint = computeLineIntersection(incidentEdge, referenceEdge[0],
          sideANormal);
    }

    Vector clippedIncidentBEndpoint = unclippedSideBEndpoint;
    if (distanceFromPlane(unclippedSideBEndpoint, referenceEdge[1], sideBNormal) > 0) {
      //compute the point of intersection of the incident edge and the side B plane
      clippedIncidentBEndpoint = computeLineIntersection(incidentEdge, referenceEdge[1],
          sideBNormal);
    }

    return new Vector[]{clippedIncidentAEndpoint, clippedIncidentBEndpoint};
  }

  /**
   * Computes the intersection point between an incident edge and a clipping plane.
   *
   * <p>Assumes the incident edge is not parallel to the plane normal and intersects with the
   * reference edge.
   * <p>
   * hand-tested: incidentEdge = [(-1, 0), (9, 10)] referencePlaneIntersection = (0, 5)
   * outwardPlaneNormal = (-1, 0) returns (1, 0)
   *
   * @param incidentEdge               edge being clipped
   * @param referencePlaneIntersection point on the clipping plane
   * @param outwardPlaneNormal         clipping plane normal
   * @return intersection point along the incident edge
   */
  private Vector computeLineIntersection(Vector[] incidentEdge, Vector referencePlaneIntersection,
      Vector outwardPlaneNormal) {
    //TODO: referencePlaneIntersection should be an endpoint of the reference edge? put in documentation
    Vector edgeDirection = incidentEdge[1].subtract(incidentEdge[0]);
    double edgeLengthInPlaneNormalDirection = edgeDirection.dot(outwardPlaneNormal);
    double fractionAlongIncidentEdge = (referencePlaneIntersection.subtract(incidentEdge[0])).dot(
        outwardPlaneNormal) / edgeLengthInPlaneNormalDirection; //

    return incidentEdge[0].add(edgeDirection.multiply(fractionAlongIncidentEdge));
  }
}
