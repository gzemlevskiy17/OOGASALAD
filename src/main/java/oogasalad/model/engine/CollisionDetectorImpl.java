package oogasalad.model.engine;

import java.util.ArrayList;
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
  private static final double GEOMETRY_EPSILON = 1e-9;

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
    collision.ifPresent(c -> {
      String aName = objectA.getClass().getSimpleName();
      String bName = objectB.getClass().getSimpleName();
      if (aName.contains("Drive") || aName.contains("Structural")
          || bName.contains("Drive") || bName.contains("Structural")) {
        System.out.println("[detect] " + aName + " vs " + bName);
      }
    });
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
    for (Vector point : polygon.points()) { //Check all points to find those that cast the biggest shadow.
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
    Optional<SatCandidate> collisionOnAxesOfA = buildCollision(objectA, objectB);
    if (collisionOnAxesOfA.isEmpty()) {
      return Optional.empty();
    }

    Optional<SatCandidate> collisionOnAxesOfB = buildCollision(objectB, objectA);
    if (collisionOnAxesOfB.isEmpty()) {
      return Optional.empty();
    }

    SatCandidate minimumTranslation =
        collisionOnAxesOfA.get().depth() <= collisionOnAxesOfB.get().depth()
            ? collisionOnAxesOfA.get()
            : collisionOnAxesOfB.get();

    Collision collision =
        new Collision(minimumTranslation.referenceObject(), minimumTranslation.incidentObject());
    collision.setPenetration(
        minimumTranslation.normal().multiply(minimumTranslation.depth()));
    collision.setReferenceEdge(minimumTranslation.referenceEdge());
    collision.setIncidentEdge(findIncidentEdge(minimumTranslation.incidentObject().getPolygon(),
        minimumTranslation.normal()));
    return Optional.of(collision);
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
  private Optional<SatCandidate> buildCollision(GameObject referenceObject,
      GameObject incidentObject) {
    //TODO: rename method (setters in the end)
    ConvexPolygon referencePolygon = referenceObject.getPolygon();
    ConvexPolygon incidentPolygon = incidentObject.getPolygon();

    double minPenetrationDepth = Double.MAX_VALUE;
    Vector normal = null;
    Vector[] minReferenceEdge = new Vector[2];
    List<Vector> referencePoints = uniqueVertices(referencePolygon);
    Vector referenceCenter = centroid(referencePoints);
    Vector incidentCenter = centroid(uniqueVertices(incidentPolygon));

    for (int i = 0; i < referencePoints.size(); i++) { // Looping over polygon edges
      Vector point1 = referencePoints.get(i);
      Vector point2 = referencePoints.get((i + 1) % referencePoints.size());
      Vector edge = point2.subtract(point1);
      if (edge.magnitude() <= GEOMETRY_EPSILON) {
        continue;
      }
      Vector outwardUnitNormal = outwardNormal(point1, point2, referenceCenter);
      if (outwardUnitNormal.dot(incidentCenter.subtract(referenceCenter)) < 0) {
        outwardUnitNormal = outwardUnitNormal.multiply(-1);
      }

      double[] projectionA = projectPolygonAlongVector(referencePolygon, outwardUnitNormal);
      double[] projectionB = projectPolygonAlongVector(incidentPolygon, outwardUnitNormal);
      boolean noOverlap =
          projectionA[1] <= projectionB[0] + GEOMETRY_EPSILON
              || projectionB[1] <= projectionA[0] + GEOMETRY_EPSILON;
      if (noOverlap) { //if the shadows don't overlap in at least one axis, there is no collision by the SAT
        return Optional.empty();
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]);
      if (currentPenetrationDepth < minPenetrationDepth) {
        normal = outwardUnitNormal;
        minPenetrationDepth = currentPenetrationDepth;
        minReferenceEdge[0] = point1;
        minReferenceEdge[1] = point2;
      }
    }

    if (normal == null) {
      return Optional.empty();
    }

    return Optional.of(new SatCandidate(referenceObject, incidentObject, normal,
        minPenetrationDepth, minReferenceEdge));
  }

  private List<Vector> uniqueVertices(ConvexPolygon polygon) {
    List<Vector> points = polygon.points();
    if (points.size() > 1 && points.getFirst().equals(points.getLast())) {
      return points.subList(0, points.size() - 1);
    }
    return points;
  }

  private Vector centroid(List<Vector> points) {
    double x = 0;
    double y = 0;
    for (Vector point : points) {
      x += point.x();
      y += point.y();
    }
    return new Vector(x / points.size(), y / points.size());
  }

  private Vector outwardNormal(Vector point1, Vector point2, Vector polygonCenter) {
    Vector edge = point2.subtract(point1);
    Vector normal = edge.normalize().rotateRight();
    Vector midpoint = point1.add(point2).multiply(0.5);
    if (normal.dot(midpoint.subtract(polygonCenter)) < 0) {
      normal = normal.multiply(-1);
    }
    return normal;
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

    List<Vector> points = uniqueVertices(incidentPolygon);
    Vector center = centroid(points);
    for (int i = 0; i < points.size(); i++) {
      Vector point1 = points.get(i);
      Vector point2 = points.get((i + 1) % points.size());
      if (point2.subtract(point1).magnitude() <= GEOMETRY_EPSILON) {
        continue;
      }
      Vector incidentNormal = outwardNormal(point1, point2, center);
      double dotWithIncident = penetrationNormal.dot(incidentNormal);
      if (dotWithIncident < minimumDotProduct) {
        minimumDotProduct = dotWithIncident;
        incidentEdge[0] = point1;
        incidentEdge[1] = point2;
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
  private Map<Vector, Double> faceClip(Vector[] clippedIncidentEdge, Vector[] referenceEdge,
      Vector referenceOutwardNormal) {
    Map<Vector, Double> manifoldPoints = new HashMap<>();
    for (Vector clippedPoint : clippedIncidentEdge) {
      double distanceFromFace = distanceFromPlane(clippedPoint, referenceEdge[0],
          referenceOutwardNormal);
      if (distanceFromFace <= GEOMETRY_EPSILON) {
        manifoldPoints.put(clippedPoint, Math.max(0.0, -distanceFromFace));
      }
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
    if (clippedIncidentEdge.length == 0) {
      collision.setManifold(new HashMap<>());
      return;
    }
    collision.setManifold(faceClip(clippedIncidentEdge, referenceEdge,
        collision.getPenetration().normalize()));
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
    Vector referenceDirection = referenceEdge[1].subtract(referenceEdge[0]).normalize();
    List<Vector> clipped = clipSegmentToPlane(List.of(incidentEdge[0], incidentEdge[1]),
        referenceDirection.multiply(-1), referenceEdge[0]);
    clipped = clipSegmentToPlane(clipped, referenceDirection, referenceEdge[1]);
    return clipped.toArray(Vector[]::new);
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
  private List<Vector> clipSegmentToPlane(List<Vector> segment, Vector outwardPlaneNormal,
      Vector planePoint) {
    if (segment.isEmpty()) {
      return List.of();
    }
    List<Vector> output = new ArrayList<>();
    Vector previous = segment.getLast();
    double previousDistance = distanceFromPlane(previous, planePoint, outwardPlaneNormal);
    for (Vector current : segment) {
      double currentDistance = distanceFromPlane(current, planePoint, outwardPlaneNormal);
      boolean currentInside = currentDistance <= GEOMETRY_EPSILON;
      boolean previousInside = previousDistance <= GEOMETRY_EPSILON;

      if (currentInside != previousInside) {
        computeLineIntersection(previous, current, planePoint, outwardPlaneNormal)
            .ifPresent(output::add);
      }
      if (currentInside) {
        output.add(current);
      }

      previous = current;
      previousDistance = currentDistance;
    }
    return output;
  }

  private Optional<Vector> computeLineIntersection(Vector start, Vector end,
      Vector referencePlaneIntersection,
      Vector outwardPlaneNormal) {
    //TODO: referencePlaneIntersection should be an endpoint of the reference edge? put in documentation
    Vector edgeDirection = end.subtract(start);
    double edgeLengthInPlaneNormalDirection = edgeDirection.dot(outwardPlaneNormal);
    if (Math.abs(edgeLengthInPlaneNormalDirection) <= GEOMETRY_EPSILON) {
      return Optional.empty();
    }
    double fractionAlongIncidentEdge = (referencePlaneIntersection.subtract(start)).dot(
        outwardPlaneNormal) / edgeLengthInPlaneNormalDirection; //

    return Optional.of(start.add(edgeDirection.multiply(fractionAlongIncidentEdge)));
  }

  private record SatCandidate(GameObject referenceObject, GameObject incidentObject, Vector normal,
                              double depth, Vector[] referenceEdge) {

  }
}
