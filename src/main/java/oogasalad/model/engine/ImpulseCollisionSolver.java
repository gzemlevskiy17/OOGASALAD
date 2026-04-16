package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;

/**
 * Impulse-based collision response implementation for a single collision pair.
 *
 * <p>Use this solver after collision detection to update linear and angular motion using contact
 * geometry, mass properties, and restitution.
 *
 * <p><strong>Assumptions:</strong> Incoming {@link Collision} objects reference valid objects and
 * states, polygon winding is consistent with helper expectations, and mass and moment-of-inertia
 * values are non-negative. Degenerate geometry or missing manifold data can cause the solver to
 * skip useful work.
 *
 * <p><strong>Dependencies:</strong> Relies on {@link Collision}, {@link ObjectState},
 * {@link GameObject}, {@link Vector}, and the {@link CollisionSolver} interface contract.
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
    List<ContactPointPhysics> collisionManifold = computeCollisionManifold(collision);
    resolveImpulseAtContacts(collision, collisionManifold);
    applyPositionalCorrection(collision);
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
  private List<ContactPointPhysics> faceClip(Vector[] clippedIncidentEdge, Vector[] referenceEdge) {
    Vector referenceOutwardNormal = referenceEdge[1].subtract(referenceEdge[0]).normalize()
        .rotateLeft();
    List<ContactPointPhysics> manifoldPoints = new ArrayList<>();
    double distanceFromLeftPlane = distanceFromPlane(clippedIncidentEdge[0], referenceEdge[0],
        referenceOutwardNormal);
    if (distanceFromLeftPlane < 0) {
      ContactPointPhysics contactPointPhysics = new ContactPointPhysics(clippedIncidentEdge[0],
          -distanceFromLeftPlane);
      manifoldPoints.add(contactPointPhysics);
    }

    double distanceFromRightPlane = distanceFromPlane(clippedIncidentEdge[1], referenceEdge[0],
        referenceOutwardNormal);
    if (distanceFromRightPlane < 0) {
      ContactPointPhysics contactPointPhysics = new ContactPointPhysics(clippedIncidentEdge[1],
          -distanceFromLeftPlane);
      manifoldPoints.add(contactPointPhysics);
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
  private List<ContactPointPhysics> computeCollisionManifold(Collision collision) {
    Vector[] incidentEdge = collision.getIncidentEdge();
    Vector[] referenceEdge = collision.getReferenceEdge();
    Vector[] clippedIncidentEdge = clipToReferenceEdge(incidentEdge, referenceEdge);
    return faceClip(clippedIncidentEdge, referenceEdge);
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

  /**
   * Applies impulse-based collision response at each manifold contact.
   *
   * <p>This updates linear and angular velocities in-place on both colliding object states.
   *
   * @param collision         detected collision pair
   * @param collisionManifold contact points derived from the collision geometry
   */
  private void resolveImpulseAtContacts(Collision collision,
      List<ContactPointPhysics> collisionManifold) {

    //TODO: refactor
    GameObject referenceObject = collision.getReferenceObject();
    GameObject incidentObject = collision.getIncidentObject();
    ObjectState referenceObjectState = referenceObject.getState();
    ObjectState incidentObjectState = incidentObject.getState();
    Vector referenceUnitNormal = collision.getPenetration().normalize();

    double totalDepth = 0;
    List<ContactPointPhysics> contactPoints = new ArrayList<>();
    for (ContactPointPhysics contactPointPhysics : collisionManifold) {
      //TODO: should this method behavior be consistent
      setRelativePositions(contactPointPhysics, incidentObjectState, referenceObjectState);

      contactPointPhysics.relativeVelocity = computeRelativeVelocity(contactPointPhysics,
          incidentObjectState,
          referenceObjectState);
      contactPointPhysics.impulse = computeImpulse(contactPointPhysics, incidentObject,
          referenceObject, referenceUnitNormal);

      totalDepth += contactPointPhysics.depth;
      contactPoints.add(contactPointPhysics);
    }

    double impulseSum = 0;
    double referenceCrossProductSum = 0;
    double incidentCrossProductSum = 0;
    for (ContactPointPhysics contactPointPhysics : contactPoints) {
      contactPointPhysics.impulse *= contactPointPhysics.depth / totalDepth;

      impulseSum += contactPointPhysics.impulse;
      referenceCrossProductSum +=
          contactPointPhysics.positionFromReference.crossWithVector(referenceUnitNormal)
              * contactPointPhysics.impulse; //TODO: make impulse vector?
      referenceCrossProductSum +=
          contactPointPhysics.positionFromIncident.crossWithVector(referenceUnitNormal)
              * contactPointPhysics.impulse;
    }

    if (invMOI(referenceObject) != 0) {
      referenceObjectState.setAngularVelocity(referenceObjectState.getAngularVelocity()
          - referenceCrossProductSum * invMOI(referenceObject));
    }
    if (invMOI(incidentObject) != 0) {
      incidentObjectState.setAngularVelocity(incidentObjectState.getAngularVelocity()
          + incidentCrossProductSum * invMOI(incidentObject));
    }

    referenceObjectState.setLinearVelocity(referenceObjectState.getLinearVelocity()
        .subtract(referenceUnitNormal.multiply(impulseSum * invMass(referenceObject))));
    incidentObjectState.setLinearVelocity(incidentObjectState.getLinearVelocity()
        .subtract(referenceUnitNormal.multiply(impulseSum * invMass(incidentObject))));
  }

  /**
   * Computes the contact-relative positions from each object's center of mass.
   *
   * @param contactPointPhysics  contact data to update
   * @param incidentObjectState  incident object state
   * @param referenceObjectState reference object state
   */
  private void setRelativePositions(ContactPointPhysics contactPointPhysics,
      ObjectState incidentObjectState, ObjectState referenceObjectState) {
    Vector incidentPoint = contactPointPhysics.contactPoint.subtract(
        incidentObjectState.getLinearPosition());
    Vector referencePoint = contactPointPhysics.contactPoint.subtract(
        referenceObjectState.getLinearPosition());
    contactPointPhysics.positionFromIncident = incidentPoint;
    contactPointPhysics.positionFromReference = referencePoint;
  }

  /**
   * Computes the scalar impulse magnitude for a single contact. Uses the formula (-1+e) * v_rel * n
   * / (1/m_reference + 1/m_incident + (r_reference x n)^2/I_reference + (r_incident x
   * n)^2/I_incident). e is the average coefficient of restitution of the two objects.
   *
   * @param contactPointPhysics contact data with relative velocity and offsets
   * @param incidentObject      incident object
   * @param referenceObject     reference object
   * @param referenceUnitNormal collision normal used for the impulse calculation
   * @return impulse magnitude along the collision normal
   */
  private double computeImpulse(ContactPointPhysics contactPointPhysics, GameObject incidentObject,
      GameObject referenceObject, Vector referenceUnitNormal) {
    double restitution = (incidentObject.getRestitution() + referenceObject.getRestitution()) / 2;
    double velocity =
        -(1 + restitution) * contactPointPhysics.relativeVelocity.dot(referenceUnitNormal);

    double incidentPointMomentum = contactPointPhysics.positionFromIncident.crossWithVector(
        referenceUnitNormal);
    double referencePointMomentum = contactPointPhysics.positionFromReference.crossWithVector(
        referenceUnitNormal);
    double inverseMass = invMass(referenceObject) + invMass(incidentObject)
        + Math.pow(incidentPointMomentum, 2) * invMOI(incidentObject)
        + Math.pow(referencePointMomentum, 2) * invMOI(referenceObject);

    return velocity / inverseMass;
  }

  /**
   * Computes the relative velocity of the incident object with respect to the reference object at a
   * contact point. Uses the formula: v_rel = (v_incident + w_incident x r_incident) - (v_reference
   * + w_reference x r_reference).
   *
   * @param contactPointPhysics  contact data to evaluate
   * @param incidentObjectState  incident object state
   * @param referenceObjectState reference object state
   * @return relative point velocity at the contact
   */
  private Vector computeRelativeVelocity(ContactPointPhysics contactPointPhysics,
      ObjectState incidentObjectState,
      ObjectState referenceObjectState) {
    Vector crossWithIncident = contactPointPhysics.positionFromIncident.crossWithScalar(
        incidentObjectState.getAngularVelocity());
    Vector incidentPointVelocity = incidentObjectState.getLinearVelocity().add(crossWithIncident);

    Vector crossWithReference = contactPointPhysics.positionFromReference.crossWithScalar(
        referenceObjectState.getAngularVelocity());
    Vector referencePointVelocity = referenceObjectState.getLinearVelocity()
        .add(crossWithReference);

    return incidentPointVelocity.subtract(referencePointVelocity);
  }

  /**
   * Moves objects out of overlap.
   *
   * <p>
   * Move incident polygon by the vector d/2 * (m_Reference/(m_A + m_B)) * B. Move reference polygon
   * by the vector -d/2 * (m_Incident/(m_A + m_B)) * B. B is a number between 0 and 1. In one step,
   * the collision is thus not fully resolved, but only partly. This method will be called multiple
   * times so that the resolution is visually smooth.
   *
   * @param collision detected collision pair
   */
  private void applyPositionalCorrection(Collision collision) {
    double correctionFraction = 0.4; //TODO: put into properties file

    GameObject referenceObject = collision.getReferenceObject();
    GameObject incidentObject = collision.getIncidentObject();
    ObjectState referenceObjectState = referenceObject.getState();
    ObjectState incidentObjectState = incidentObject.getState();

    double totalInvMass = invMass(referenceObject) + invMass(incidentObject);
    if (totalInvMass == 0) {
      return;
    }

    Vector penetration = collision.getPenetration();
    double magnitude = penetration.magnitude() * correctionFraction / totalInvMass;
    Vector correction = penetration.normalize().multiply(magnitude);

    referenceObjectState.setLinearPosition(referenceObjectState.getLinearPosition()
        .subtract(correction.multiply(invMass(referenceObject))));
    incidentObjectState.setLinearPosition(incidentObjectState.getLinearPosition()
        .add(correction.multiply(invMass(incidentObject))));
  }

  private static double invMass(GameObject obj) {
    return obj.getMass() == 0 ? 0.0 : 1.0 / obj.getMass();
  }

  private static double invMOI(GameObject obj) {
    return obj.getMomentOfInertia() == 0 ? 0.0 : 1.0 / obj.getMomentOfInertia();
  }

  /**
   * Physics data for a contact point in a collision manifold used while resolving a single
   * collision.
   */
  class ContactPointPhysics {

    Vector contactPoint;
    Vector positionFromIncident;
    Vector positionFromReference;
    Vector relativeVelocity;
    double depth;
    double impulse;

    /**
     * Creates contact data for a clipped collision point.
     *
     * @param contactPoint world-space contact point
     * @param depth        penetration depth at the contact point
     */
    public ContactPointPhysics(Vector contactPoint, double depth) {
      this.contactPoint = contactPoint;
      this.depth = depth;
    }
  }
}
