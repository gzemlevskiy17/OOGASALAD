package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.RigidBody;

public class ImpulseSolverUtilities {
//TODO: comment
  /**
   * Computes the contact-relative positions from each object's center of mass.
   *
   * @param contactPointPhysics  contact data to update
   * @param incidentStaticState  incident object state
   * @param referenceStaticState reference object state
   */
  private static void setRelativePositions(ContactPointPhysics contactPointPhysics,
      StaticState incidentStaticState, StaticState referenceStaticState) {
    Vector incidentPoint = contactPointPhysics.contactPoint.subtract(
        incidentStaticState.getLinearPosition());
    Vector referencePoint = contactPointPhysics.contactPoint.subtract(
        referenceStaticState.getLinearPosition());
    contactPointPhysics.positionFromIncident = incidentPoint;
    contactPointPhysics.positionFromReference = referencePoint;
  }

  /**
   * Computes the scalar impulse magnitude for a single contact. Uses the formula (-1+e) * v_rel * n
   * / (1/m_reference + 1/m_incident + (r_reference x n)^2/I_reference + (r_incident x
   * n)^2/I_incident). e is the average coefficient of restitution of the two objects.
   *
   * @param contactPointPhysics contact data with relative velocity and offsets
   * @param referenceUnitNormal collision normal used for the impulse calculation
   * @return impulse magnitude along the collision normal
   */
  private static double computeImpulse(RigidBody referenceObject, RigidBody incidentObject,
      ContactPointPhysics contactPointPhysics,
      Vector referenceUnitNormal) {
    double restitution = (incidentObject.getRestitution() + referenceObject.getRestitution()) / 2;
    double velocity =
        -(1 + restitution) * contactPointPhysics.relativeVelocity.dot(referenceUnitNormal);

    double incidentPointMomentum = contactPointPhysics.positionFromIncident.crossWithVector(
        referenceUnitNormal);
    double referencePointMomentum = contactPointPhysics.positionFromReference.crossWithVector(
        referenceUnitNormal);
    double inverseMass = referenceObject.getInverseMass() + incidentObject.getInverseMass()
        + Math.pow(incidentPointMomentum, 2) * incidentObject.getInverseMomentOfInertia()
        + Math.pow(referencePointMomentum, 2) * referenceObject.getInverseMomentOfInertia();

    return velocity / inverseMass;
  }

  /**
   * Computes the relative velocity of the incident object with respect to the reference object at a
   * contact point. Uses the formula: v_rel = (v_incident + w_incident x r_incident) - (v_reference
   * + w_reference x r_reference).
   *
   * @param contactPointPhysics   contact data to evaluate
   * @param incidentDynamicState  incident object state
   * @param referenceDynamicState reference object state
   * @return relative point velocity at the contact
   */
  private static Vector computeRelativeVelocity(ContactPointPhysics contactPointPhysics,
      DynamicState incidentDynamicState,
      DynamicState referenceDynamicState) {
    Vector crossWithIncident = contactPointPhysics.positionFromIncident.crossWithScalar(
        incidentDynamicState.getAngularVelocity());
    Vector incidentPointVelocity = incidentDynamicState.getLinearVelocity().add(crossWithIncident);

    Vector crossWithReference = contactPointPhysics.positionFromReference.crossWithScalar(
        referenceDynamicState.getAngularVelocity());
    Vector referencePointVelocity = referenceDynamicState.getLinearVelocity()
        .add(crossWithReference);

    return incidentPointVelocity.subtract(referencePointVelocity);
  }

  /**
   * Moves objects out of overlap.
   *
   * <p>Uses Baumgarte-style positional projection: each frame resolves {@code correctionFraction}
   * of the remaining penetration (above a small {@code slop} tolerance), and splits the motion
   * between the two bodies proportionally to their inverse masses. Static bodies (inverse mass 0)
   * therefore absorb none of the correction, while lighter dynamic bodies absorb more than heavier
   * ones. This method is expected to be called multiple times per frame (either by the solver
   * iteration loop or by the joint/collision interleave in the engine) so the remaining penetration
   * decays geometrically to zero.
   *
   * @param collision       detected collision pair; penetration must point from the reference body
   *                        outward toward the incident body (true MTV direction)
   * @param referenceObject body that receives the negative share of the correction
   * @param incidentObject  body that receives the positive share of the correction
   */
  public static void applyPositionalCorrection(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject) {
    final double correctionFraction = 0.4; // TODO: put into properties file
    final double slop = 0.01;              // ignore sub-pixel overlaps to avoid jitter

    double totalInverseMass =
        incidentObject.getInverseMass() + referenceObject.getInverseMass();
    if (totalInverseMass == 0.0) {
      return; // two static bodies: nothing movable to correct
    }

    Vector penetration = collision.getPenetration();
    double depth = penetration.magnitude();
    if (depth <= slop) {
      return;
    }

    Vector normal = penetration.multiply(1.0 / depth); // normalize without re-reading magnitude
    double correctionMagnitude = (depth - slop) * correctionFraction / totalInverseMass;
    Vector correction = normal.multiply(correctionMagnitude);

    StaticState referenceStaticState = referenceObject.getStaticState();
    StaticState incidentStaticState = incidentObject.getStaticState();

    if (referenceObject.getInverseMass() > 0.0) {
      Vector newReferencePosition = referenceStaticState.getLinearPosition()
          .subtract(correction.multiply(referenceObject.getInverseMass()));
      referenceStaticState.setLinearPosition(newReferencePosition);
    }

    if (incidentObject.getInverseMass() > 0.0) {
      Vector newIncidentPosition = incidentStaticState.getLinearPosition()
          .add(correction.multiply(incidentObject.getInverseMass()));
      incidentStaticState.setLinearPosition(newIncidentPosition);
    }
  }

  public static void resolveImpulseAtContactsHelper(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject, StaticState incidentStaticState,
      StaticState referenceStaticState, DynamicState incidentDynamicState,
      DynamicState referenceDynamicState) {
    Vector referenceUnitNormal = collision.getPenetration().normalize();

    double totalDepth = 0;
    List<ContactPointPhysics> contactPoints = new ArrayList<>(); //TODO: rename
    for (Vector contactPoint : collision.getCollisionManifold().keySet()) {
      //TODO: should these method behaviors be consistent
      ContactPointPhysics contactPointPhysics = new ContactPointPhysics(contactPoint,
          collision.getCollisionManifold().get(contactPoint));
      ImpulseSolverUtilities.setRelativePositions(contactPointPhysics, incidentStaticState,
          referenceStaticState);

      contactPointPhysics.relativeVelocity = ImpulseSolverUtilities.computeRelativeVelocity(
          contactPointPhysics,
          incidentDynamicState, referenceDynamicState);
      contactPointPhysics.impulse = computeImpulse(referenceObject, incidentObject,
          contactPointPhysics,
          referenceUnitNormal);

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
      incidentCrossProductSum +=
          contactPointPhysics.positionFromIncident.crossWithVector(referenceUnitNormal)
              * contactPointPhysics.impulse;
    }

    double newReferenceAngularVelocity = referenceDynamicState.getAngularVelocity()
        - referenceCrossProductSum * referenceObject.getInverseMomentOfInertia();
    referenceDynamicState.setAngularVelocity(newReferenceAngularVelocity);

    double newIncidentAngularVelocity = incidentDynamicState.getAngularVelocity()
        + incidentCrossProductSum * incidentObject.getInverseMomentOfInertia();
    incidentDynamicState.setAngularVelocity(newIncidentAngularVelocity);

    Vector newReferenceLinearVelocity = referenceDynamicState.getLinearVelocity()
        .subtract(referenceUnitNormal.multiply(impulseSum * referenceObject.getInverseMass()));
    referenceDynamicState.setLinearVelocity(newReferenceLinearVelocity);

    Vector newIncidentLinearVelocity = incidentDynamicState.getLinearVelocity()
        .add(referenceUnitNormal.multiply(impulseSum * incidentObject.getInverseMass()));
    incidentDynamicState.setLinearVelocity(newIncidentLinearVelocity);
  }
}
