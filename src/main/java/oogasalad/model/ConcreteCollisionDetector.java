package oogasalad.model;

import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;

//TODO: delegate responsibility
public class ConcreteCollisionDetector implements CollisionDetector {

  @Override
  public void detectCollisions(World world) {

  }

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


  private Collision collisionDetected(GameObject objectA, GameObject objectB) {
    Collision collisionOnAxesOfA = penetrationDepthOnAxesOfA(objectA, objectB);
    if (collisionOnAxesOfA.isCollided()) {
      return collisionOnAxesOfA;
    }
    return penetrationDepthOnAxesOfA(objectB, objectA);
  }

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
