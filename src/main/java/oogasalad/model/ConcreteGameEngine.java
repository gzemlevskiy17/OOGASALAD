package oogasalad.model;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;

public class ConcreteGameEngine implements GameEngine {

  private static final int MAX_CONTACT_POINTS = 2;
  private World world;

  public ConcreteGameEngine(World world) {
    this.world = world;
  }

  //TODO: implement collision detection for all world objects
  //TODO: implement application of forces
  @Override
  public void step(double elapsedTime) {
//    for (Joint joint :  world.getJoints()) {
//
//    }

    for (GameObject gameObject : world.getObjects()) { //update to include only dynamic rigid bodies
      ObjectState newState = integrateObjectState(gameObject.getState(), elapsedTime);
      gameObject.setState(newState);
    }

//    solveCollisions();

//    for (Constraint constraint : constraings) {
//      enforceConstraint(constraint);
//    }

  }

  private Vector[] computeCollisionManifold(Collision collision, Vector refNormal) {
    Vector[] incidentEdge = collision.getIncidentEdge();
    Vector[] referenceEdge = collision.getReferenceEdge();
    if (incidentEdge == null || referenceEdge == null) {
      return new Vector[MAX_CONTACT_POINTS];
    }

    Vector referenceStart = referenceEdge[0];
    Vector referenceEnd = referenceEdge[1];
    Vector tangent = referenceEnd.subtract(referenceStart).normalize();
    double leftOffset = tangent.dot(referenceStart);
    double rightOffset = tangent.multiply(-1).dot(referenceEnd);

    List<Vector> leftClip = clipToHalfSpace(incidentEdge, tangent, leftOffset);
    if (leftClip.size() < 2) {
      return new Vector[MAX_CONTACT_POINTS];
    }

    Vector[] leftClippedEdge = new Vector[]{leftClip.get(0), leftClip.get(1)};
    List<Vector> sideClip = clipToHalfSpace(leftClippedEdge, tangent.multiply(-1), rightOffset);

    Vector[] finalManifold = new Vector[MAX_CONTACT_POINTS];
    int manifoldCount = 0;
    double faceOffset = refNormal.dot(referenceStart);
    for (Vector vector : sideClip) {
      double separation = refNormal.dot(vector) - faceOffset;
      if (separation <= 0 && manifoldCount < MAX_CONTACT_POINTS) {
        finalManifold[manifoldCount++] = vector;
      }
    }

    return finalManifold;
  }

  //TODO: confirm direction of normals
  private Vector[] incidentEdge(ConvexPolygon incidentPolygon, Vector penetrationNormal) {
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

  //TODO: potentially refactor into one clip from both sides, or to return a clipped line segment out of which a manifold is constructed
  private List<Vector> clipToHalfSpace(Vector[] incidentEdge, Vector planeNormal,
      double planeOffset) {
    Vector incidentStart = incidentEdge[0];
    Vector incidentEnd = incidentEdge[1];

    List<Vector> result = new ArrayList<>();
    double distanceFromIncidentStart = planeNormal.dot(incidentStart) - planeOffset;
    double distanceFromIncidentEnd = planeNormal.dot(incidentEnd) - planeOffset;

    if (distanceFromIncidentStart >= 0.0) {
      addCandidatePointToManifold(result, incidentStart);
    }
    if (distanceFromIncidentEnd >= 0.0) {
      addCandidatePointToManifold(result, incidentEnd);
    }

    if (distanceFromIncidentStart * distanceFromIncidentEnd < 0.0) {
      double fractionOfIncidentEdge =
          distanceFromIncidentStart / (distanceFromIncidentStart - distanceFromIncidentEnd);
      Vector intersection = incidentStart.add(
          incidentEnd.subtract(incidentStart).multiply(fractionOfIncidentEdge));
      addCandidatePointToManifold(result, intersection);
    }

    return result;
  }

  private void addCandidatePointToManifold(List<Vector> points, Vector point) {
    if (points.size() < MAX_CONTACT_POINTS) {
      points.add(point);
    }
  }

  //TODO: refactor
  private void resolveImpulseAtContacts(
      Collision collision,
      Vector normal,
      Vector[] contactPoints,
      int contactCount) {
    GameObject objectA = collision.getObjectA();
    GameObject objectB = collision.getObjectB();
    ObjectState stateA = objectA.getState();
    ObjectState stateB = objectB.getState();

    Vector velocityA = stateA.getLinearVelocity();
    Vector velocityB = stateB.getLinearVelocity();
    double angVelocityA = stateA.getAngularVelocity().x();
    double angVelocityB = stateB.getAngularVelocity().x();

    double invMassA = inverseOrZero(objectA.getMass());
    double invMassB = inverseOrZero(objectB.getMass());
    double invInertiaA = inverseOrZero(objectA.getMomentOfInertia());
    double invInertiaB = inverseOrZero(objectB.getMomentOfInertia());
    double restitution = Math.min(objectA.getRestitution(), objectB.getRestitution());

    int effectiveContactCount = Math.min(contactCount, contactPoints.length);
    if (effectiveContactCount <= 0) {
      return;
    }

    for (int i = 0; i < effectiveContactCount; i++) {
      Vector contactPoint = contactPoints[i];
      if (contactPoint == null) {
        continue;
      }

      Vector ra = contactPoint.subtract(stateA.getLinearPosition());
      Vector rb = contactPoint.subtract(stateB.getLinearPosition());

      Vector combinedVelocityA = velocityA.add(ra.perpendicular().multiply(angVelocityA));
      Vector combinedVelocityB = velocityB.add(rb.perpendicular().multiply(angVelocityB));
      Vector relativeVelocity = combinedVelocityB.subtract(combinedVelocityA);

      double contactVelocity = relativeVelocity.dot(normal);
      if (contactVelocity > 0.0) {
        continue;
      }

      double raCrossN = ra.cross(normal);
      double rbCrossN = rb.cross(normal);
      double denominator = invMassA + invMassB
          + (raCrossN * raCrossN * invInertiaA)
          + (rbCrossN * rbCrossN * invInertiaB);
      if (denominator == 0.0) {
        continue;
      }

      double impulseScalar = -(1.0 + restitution) * contactVelocity;
      impulseScalar /= denominator * effectiveContactCount;
      Vector impulse = normal.multiply(impulseScalar);

      velocityA = velocityA.subtract(impulse.multiply(invMassA));
      angVelocityA -= raCrossN * impulseScalar * invInertiaA;
      velocityB = velocityB.add(impulse.multiply(invMassB));
      angVelocityB += rbCrossN * impulseScalar * invInertiaB;
    }

    stateA.setLinearVelocity(velocityA);
    stateB.setLinearVelocity(velocityB);

    stateA.setAngularVelocity(new Vector(angVelocityA, 0));
    stateB.setAngularVelocity(new Vector(angVelocityB, 0));
  }


  //TODO: refactor
  private void applyPositionalCorrection(
      Collision collision,
      Vector normal,
      double[] depths,
      int contactCount) {
    final double percent = 0.4;
    final double slop = 0.01;

    GameObject objectA = collision.getObjectA();
    GameObject objectB = collision.getObjectB();
    ObjectState stateA = objectA.getState();
    ObjectState stateB = objectB.getState();

    double invMassA = inverseOrZero(objectA.getMass());
    double invMassB = inverseOrZero(objectB.getMass());
    double inverseMassSum = invMassA + invMassB;
    if (inverseMassSum == 0.0 || contactCount <= 0 || depths.length == 0) {
      return;
    }

    int effectiveContactCount = Math.min(contactCount, depths.length);
    double maxDepth = 0.0;
    for (int i = 0; i < effectiveContactCount; i++) {
      maxDepth = Math.max(maxDepth, depths[i]);
    }

    double correctionMagnitude = Math.max(maxDepth - slop, 0.0);
    correctionMagnitude = (correctionMagnitude / inverseMassSum) * percent;
    Vector correction = normal.multiply(correctionMagnitude);

    stateA.setLinearPosition(stateA.getLinearPosition().subtract(correction.multiply(invMassA)));
    stateB.setLinearPosition(stateB.getLinearPosition().add(correction.multiply(invMassB)));
  }

  //TODO: is this needed?
  private double inverseOrZero(double value) {
    if (value <= 0.0) {
      return 0.0;
    }
    return 1.0 / value;
  }

  //TODO: implement
  private ObjectState integrateObjectState(ObjectState state, double elapsedTime) {
    return state;
  }
}
