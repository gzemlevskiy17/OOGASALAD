package oogasalad.model.engine;

import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * SAT-based narrow-phase collision detector implementation.
 *
 * <p>Use this class to evaluate whether two convex polygon objects overlap and to derive
 * penetration data for downstream resolution.
 *
 * <p><strong>Assumptions:</strong> Polygons are convex and provide points in consistent winding
 * order. Pair selection and world-wide iteration are intentionally out of scope for this class and
 * belong to higher-level engine flow.
 *
 * <p><strong>Dependencies:</strong> Relies on {@link GameObject}, {@link ConvexPolygon},
 * {@link Vector}, and the model {@link Collision} data container.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionDetector detector = new CollisionDetectorImpl();
 * detector.detectCollisions(objectA, objectB);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class CollisionDetectorImpl implements CollisionDetector {
  //TODO: test & refactor
  //TODO: tie together
  //TODO: update javadoc once complete

  /**
   * Evaluates a single object pair for SAT overlap.
   *
   * @param objectA first object in the pair
   * @param objectB second object in the pair
   * @return an {@link Optional} containing the detected collision, or
   *     {@link Optional#empty()} when the shapes do not overlap
   */
  @Override
  public Optional<Collision> detectCollision(GameObject objectA, GameObject objectB) {
    return findSatCollision(objectA, objectB);
  }
  //TODO: figure out how Collision will be returned if no collision was detected

  /**
   * Projects all polygon vertices onto the given axis and returns [min, max] values of the
   * endpoints of the shadow of the shape along the normal. The shadow is cast in the tangent
   * direction, so the endpoints form a line segment parallel to the normal.
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
   * Performs SAT checks on both polygon axis sets and returns the first separating-axis failure or
   * minimum-penetration collision.
   */
  private Optional<Collision> findSatCollision(GameObject objectA, GameObject objectB) {
    Optional<Collision> collisionOnAxesOfA = penetrationDepthOnAxesOfA(objectA, objectB);
    if (collisionOnAxesOfA.isPresent()) {
      return collisionOnAxesOfA;
    }
    return penetrationDepthOnAxesOfA(objectB, objectA);
  }

  /**
   * Detects collisions using the SAT along the edges of {@code objectA} and tracks the smallest
   * penetration vector across those edges.
   */
  private Optional<Collision> penetrationDepthOnAxesOfA(GameObject objectA, GameObject objectB) {
    ConvexPolygon polygonA = objectA.getPolygon();
    ConvexPolygon polygonB = objectB.getPolygon();

    Collision collision = new Collision(objectA, objectB, new Vector(0, 0));
    double minPenetrationDepth = Double.MAX_VALUE;
    for (int i = 0; i < polygonA.getPoints().size() - 1; i++) { // Looping over edges clockwise
      Vector point1 = polygonA.getPoints().get(i);
      Vector point2 = polygonA.getPoints().get(i + 1);
      Vector tangent = point2.subtract(point1).normalize();
      Vector normal = tangent.rotateRight(); //Find the inward normal along which to compute the penetration depth

      double[] projectionA = projectPolygonAlongVector(polygonA, normal);
      double[] projectionB = projectPolygonAlongVector(polygonB, normal);
      boolean shadowsOverlap = projectionA[1] <= projectionB[0] || projectionB[1] <= projectionA[0];
      if (shadowsOverlap) { //if the shadows don't overlap in at least one axis, there is no collision by the SAT
        return Optional.empty();
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]);
      if (currentPenetrationDepth < minPenetrationDepth) {
        collision.setPenetration(normal.multiply(currentPenetrationDepth));
        minPenetrationDepth = currentPenetrationDepth;
      }
    }
    return Optional.of(collision);
  }
}
