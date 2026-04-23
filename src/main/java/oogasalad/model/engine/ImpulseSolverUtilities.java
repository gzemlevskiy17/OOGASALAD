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
   * <p>
   * Move incident polygon by the vector d/2 * (m_Reference/(m_A + m_B)) * B. Move reference polygon
   * by the vector -d/2 * (m_Incident/(m_A + m_B)) * B. B is a number between 0 and 1. In one step,
   * the collision is thus not fully resolved, but only partly. This method will be called multiple
   * times so that the resolution is visually smooth.
   *
   * @param collision detected collision pair
   */
  public static void applyPositionalCorrection(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject) {
    double correctionFraction = 0.4; //TODO: put into properties file

//    GameObject referenceObject = collision.getReferenceObject();
//    GameObject incidentObject = collision.getIncidentObject();
    StaticState referenceStaticState = referenceObject.getStaticState();
    StaticState incidentStaticState = incidentObject.getStaticState();

    double inverseMassSum = incidentObject.getInverseMass() + referenceObject.getInverseMass();

    Vector penetration = collision.getPenetration();
    double correctionMagnitude =
        (penetration.magnitude() / 2) * inverseMassSum * correctionFraction;
    Vector correction = penetration.normalize().multiply(correctionMagnitude);

    Vector newReferenceObjectState = referenceStaticState.getLinearPosition()
        .subtract(correction.multiply(referenceObject.getInverseMass()));
    referenceStaticState.setLinearPosition(newReferenceObjectState);

    Vector newIncidentObjectState = incidentStaticState.getLinearPosition()
        .add(correction.multiply(incidentObject.getInverseMass()));
    incidentStaticState.setLinearPosition(newIncidentObjectState);
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
