package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Impulse-based collision response implementation for a single collision pair.
 *
 * <p>Use this solver when a collision has already been detected and you want physically motivated
 * velocity updates (and optional overlap correction) driven by contact geometry and material
 * properties.
 *
 * <p><strong>Assumptions:</strong> Incoming {@link Collision} objects reference valid objects and
 * states, polygon winding is consistent with helper expectations, and mass/inertia values are
 * non-negative. Degenerate geometry or missing manifold data can cause the solver to no-op.
 *
 * <p><strong>Dependencies:</strong> Relies on {@link Collision}, {@link ObjectState},
 * {@link ConvexPolygon}, {@link GameObject}, {@link Vector}, and the {@link CollisionSolver}
 * interface contract.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionSolver solver = new ImpulseCollisionSolver();
 * solver.solve(collision);
 * }</pre>
 *
 * <p><strong>Details:</strong> This class resolves one pair at a time. Pair scheduling and
 * iteration order are handled by the engine, not by this solver.
 *
 * @author Georgiy Zemlevskiy
 */
public class ImpulseCollisionSolver implements CollisionSolver {

  public final static int MAX_CONTACT_POINTS = 2; //TODO: put in properties file

  /**
   * Resolves one collision using the impulse solver pipeline.
   *
   * <p>Use this method from engine collision orchestration after detection has produced per-pair
   * collision data for the current step.
   *
   * <p><strong>Assumptions:</strong> {@code collision} is non-null and contains enough contact and
   * normal information for response calculations.
   *
   * @param collision pairwise collision record to resolve
   */
  @Override
  public void solve(Collision collision) {

  }

  /**
   * Clips an incident edge that has been clipped against the reference edge by width, against the
   * reference edge by depth. That is, it adds endpoints of {@code clippedIncidentEdge} to the
   * collision manifold if they inside the reference polygon ('below' the reference edge). Assume
   * that at least one endpoint of the clipped incident edge lies below the reference edge and that
   * reference edge has preserved its order (CW)
   */
  private List<Vector> faceClip(Vector[] clippedIncidentEdge, Vector[] referenceEdge) {
    Vector referenceOutwardNormal = referenceEdge[1].subtract(referenceEdge[0]).normalize()
        .rotateLeft();
    List<Vector> manifoldPoints = new ArrayList<>();
    if (clippedIncidentEdge[0].subtract(referenceEdge[0]).dot(referenceOutwardNormal) < 0) {
      manifoldPoints.add(clippedIncidentEdge[0]);
    }

    if (clippedIncidentEdge[1].subtract(referenceEdge[0]).dot(referenceOutwardNormal) < 0) {
      manifoldPoints.add(clippedIncidentEdge[1]);
    }

    return manifoldPoints;
  }
  //TODO: delegate plane offset method
  //hand-tested: clippedIncidentEdge = [(0, 1), (8, 9)] referenceEdge = [(0, 5), (8, 5)] referenceOutwardNormal = (0, 1)
  // only adds (0, 1) to manifold, not (8, 9), which is not in the reference polygon.

  private List<Vector> computeCollisionManifold(Collision collision) {
    Vector[] incidentEdge = collision.getIncidentEdge();
    Vector[] referenceEdge = collision.getReferenceEdge();
    Vector[] clippedIncidentEdge = clipToReferenceEdge(incidentEdge, referenceEdge);
    return faceClip(clippedIncidentEdge, referenceEdge);
  }


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
    if (unclippedSideAEndpoint.subtract(referenceEdge[0]).dot(sideANormal) > 0) {
      //compute the point of intersection of the incident edge and the side A plane
      clippedIncidentAEndpoint = computeLineIntersection(incidentEdge, referenceEdge[0],
          sideANormal);
    }

    Vector clippedIncidentBEndpoint = unclippedSideBEndpoint;
    if (unclippedSideAEndpoint.subtract(referenceEdge[1]).dot(sideBNormal) > 0) {
      //compute the point of intersection of the incident edge and the side B plane
      clippedIncidentBEndpoint = computeLineIntersection(incidentEdge, referenceEdge[1],
          sideBNormal);
    }

    return new Vector[]{clippedIncidentAEndpoint, clippedIncidentBEndpoint};
  }

  private Vector computeLineIntersection(Vector[] incidentEdge, Vector referencePlaneIntersection,
      Vector outwardPlaneNormal) {
    //TODO: referencePlaneIntersection should be endpoint
    Vector edgeDirection = incidentEdge[1].subtract(incidentEdge[0]);
    double edgeLengthInPlaneNormalDirection = edgeDirection.dot(outwardPlaneNormal);
//    if (edgeLengthInPlaneNormalDirection == 0.0) {
//      return null;
//    }
    double fractionAlongIncidentEdge = (referencePlaneIntersection.subtract(incidentEdge[0])).dot(
        outwardPlaneNormal) / edgeLengthInPlaneNormalDirection; //
//    if (fractionAlongIncidentEdge < 0.0 || fractionAlongIncidentEdge > 1.0) {
//      return null;
//    }
    return incidentEdge[0].add(edgeDirection.multiply(fractionAlongIncidentEdge));
  }
  //hand-tested: incidentEdge = [(-1, 0), (9, 10)] referencePlaneIntersection = (0, 5) outwardPlaneNormal = (-1, 0) returns (1, 0)

  /**
   * Applies impulse-based collision response at each manifold contact, updating linear and angular
   * velocities in-place on both colliding object states.
   */
  private void resolveImpulseAtContacts(Collision collision, Vector normal,
      List<Vector> collisionManifold) {

    //TODO: refactor
    GameObject objectA = collision.getObjectA();
    GameObject objectB = collision.getObjectB();
    ObjectState stateA = objectA.getState();
    ObjectState stateB = objectB.getState();

    Vector velocityA = stateA.getLinearVelocity();
    Vector velocityB = stateB.getLinearVelocity();
    double angVelocityA = stateA.getAngularVelocity().x();
    double angVelocityB = stateB.getAngularVelocity().x();

    double invMassA = 1 / objectA.getMass();
    double invMassB = 1 / objectB.getMass();
    double invInertiaA = objectA.getMomentOfInertia();
    double invInertiaB = objectB.getMomentOfInertia();
    double restitution = Math.min(objectA.getRestitution(), objectB.getRestitution());

    for (Vector contactPoint : collisionManifold) {
      Vector ra = contactPoint.subtract(stateA.getLinearPosition());
      Vector rb = contactPoint.subtract(stateB.getLinearPosition());

      Vector combinedVelocityA = velocityA.add(ra.rotateLeft().multiply(angVelocityA));
      Vector combinedVelocityB = velocityB.add(rb.rotateLeft().multiply(angVelocityB));
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
      impulseScalar /= denominator * collisionManifold.size();
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

  //TODO: add formula to comment

  /**
   * Moves objects out of overlap according to {formula}.
   */
  private void applyPositionalCorrection(
      Collision collision,
      Vector normal,
      double[] depths,
      int contactCount) {

    //TODO: refactor
    final double percent = 0.4;
    final double slop = 0.01;

    GameObject objectA = collision.getObjectA();
    GameObject objectB = collision.getObjectB();
    ObjectState stateA = objectA.getState();
    ObjectState stateB = objectB.getState();

    double invMassA = 1 / objectA.getMass();
    double invMassB = 1 / objectB.getMass();
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
//  private double inverseOrZero(double value) {
//    if (value <= 0.0) {
//      return 0.0;
//    }
//    return 1.0 / value;
//  }
}
