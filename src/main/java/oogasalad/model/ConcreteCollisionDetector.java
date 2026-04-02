package oogasalad.model;

import java.util.List;
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
 * CollisionDetector detector = new ConcreteCollisionDetector();
 * detector.detectCollisions(objectA, objectB);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class ConcreteCollisionDetector implements CollisionDetector {

  //TODO: update javadoc once complete
  /**
   * Evaluates a single object pair for SAT overlap.
   *
   *
   * @param objectA first object in the pair
   * @param objectB second object in the pair
   * @return pairwise collision records; currently {@code null} until implementation is completed
   */
  @Override
  public Collision detectCollision(GameObject objectA, GameObject objectB) {
    return null;
  }

  /**
   * Projects all polygon vertices onto the given axis and returns [min, max] interval values.
   */
  private double[] projectPolygonAlongVector(ConvexPolygon polygon, Vector normal) {
    double[] projection = new double[2];
    projection[0] = Double.MAX_VALUE;
    projection[1] = -Double.MAX_VALUE;
    for (Vector point : polygon.getPoints()) {
      double direction = normal.dot(point); //TODO: confirm variable name
      projection[0] = Math.min(direction, projection[0]);
      projection[1] = Math.max(direction, projection[1]);
    }

    return projection;
  }

  /**
   * Performs SAT checks on both polygon axis sets and returns the first separating-axis failure or
   * minimum-penetration collision.
   */
  private Collision collisionDetected(GameObject objectA, GameObject objectB) {
    Collision collisionOnAxesOfA = penetrationDepthOnAxesOfA(objectA, objectB);
    if (collisionOnAxesOfA.isCollided()) {
      return collisionOnAxesOfA;
    }
    return penetrationDepthOnAxesOfA(objectB, objectA);
  }

  /**
   * Tests overlap using only axes derived from {@code objectA} and tracks the smallest
   * penetration vector across those axes.
   */
  private Collision penetrationDepthOnAxesOfA(GameObject objectA, GameObject objectB) {
    ConvexPolygon polygonA = objectA.getPolygon();
    ConvexPolygon polygonB = objectB.getPolygon();

    Collision collision = new Collision(objectA, objectB, true, new Vector(0, 0));
    double minPenetrationDepth = Double.MAX_VALUE;
    for (int i = 0; i < polygonA.getPoints().size() - 1; i++) {
      Vector point1 = polygonA.getPoints().get(i);
      Vector point2 = polygonA.getPoints().get(i + 1);
      Vector tangent = point2.subtract(point1).normalize();
      Vector normal = tangent.rotateLeft();

      double[] projectionA = projectPolygonAlongVector(polygonA, normal);
      double[] projectionB = projectPolygonAlongVector(polygonB, normal);
      if (projectionA[1] <= projectionB[0] || projectionB[1] <= projectionA[0]) {
        collision.setCollided(false);
        return collision;
      }

      double currentPenetrationDepth =
          Math.min(projectionA[1], projectionB[1]) - Math.max(projectionA[0],
              projectionB[0]); //TODO: verify
      if (currentPenetrationDepth < minPenetrationDepth) {
        collision.setPenetration(normal.multiply(currentPenetrationDepth));
        minPenetrationDepth = currentPenetrationDepth;
      }
    }
    return collision;
  }
}
