package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.RigidBody;

public class ImpulseSolverUtilities {
//TODO: comment
  private static final double SOLVER_EPSILON = 1e-9;

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
  /**
   * Below this approach-speed threshold, contacts are treated as perfectly inelastic (e=0).
   * Prevents resting contacts from endlessly bouncing when gravity adds a small approach velocity
   * every frame that restitution would otherwise reflect into a persistent oscillation. Set above a
   * single gravity impulse ({@code g*dt ~= 1.6 px/s} at 60 Hz, g=98) but well under any real landing
   * or collision velocity.
   */
  private static final double RESTITUTION_APPROACH_THRESHOLD = 40.0;

  private static double computeImpulseDenominator(RigidBody referenceObject,
      RigidBody incidentObject, ContactPointPhysics contactPointPhysics, Vector unitDirection) {
    double incidentPointMomentum = contactPointPhysics.positionFromIncident.crossWithVector(
        unitDirection);
    double referencePointMomentum = contactPointPhysics.positionFromReference.crossWithVector(
        unitDirection);
    return referenceObject.getInverseMass() + incidentObject.getInverseMass()
        + Math.pow(incidentPointMomentum, 2) * incidentObject.getInverseMomentOfInertia()
        + Math.pow(referencePointMomentum, 2) * referenceObject.getInverseMomentOfInertia();
  }

  private static double computeImpulse(RigidBody referenceObject, RigidBody incidentObject,
      ContactPointPhysics contactPointPhysics,
      Vector referenceUnitNormal) {
    double restitution = (incidentObject.getRestitution() + referenceObject.getRestitution()) / 2;
    double approach = -contactPointPhysics.relativeVelocity.dot(referenceUnitNormal);
    if (approach < RESTITUTION_APPROACH_THRESHOLD) {
      restitution = 0;
    }
    double velocity = (1 + restitution) * approach;
    if (velocity <= 0) {
      return 0;
    }

    double inverseMass = computeImpulseDenominator(referenceObject, incidentObject,
        contactPointPhysics, referenceUnitNormal);
    if (inverseMass <= SOLVER_EPSILON) {
      return 0;
    }

    return velocity / inverseMass;
  }

  private static double computeFrictionImpulse(RigidBody referenceObject, RigidBody incidentObject,
      ContactPointPhysics contactPointPhysics, Vector referenceUnitNormal, double normalImpulse) {
    Vector tangent = contactPointPhysics.relativeVelocity.subtract(referenceUnitNormal.multiply(
        contactPointPhysics.relativeVelocity.dot(referenceUnitNormal)));
    double tangentMagnitude = tangent.magnitude();
    if (tangentMagnitude <= SOLVER_EPSILON || normalImpulse <= 0.0) {
      return 0.0;
    }

    Vector unitTangent = tangent.multiply(1.0 / tangentMagnitude);
    double inverseMass = computeImpulseDenominator(referenceObject, incidentObject,
        contactPointPhysics, unitTangent);
    if (inverseMass <= SOLVER_EPSILON) {
      return 0.0;
    }

    double tangentImpulse = -contactPointPhysics.relativeVelocity.dot(unitTangent) / inverseMass;
    double friction = Math.sqrt(referenceObject.getFriction() * incidentObject.getFriction());
    double maximumFriction = normalImpulse * friction;
    return Math.max(-maximumFriction, Math.min(tangentImpulse, maximumFriction));
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

  private static void applyImpulse(RigidBody referenceObject, RigidBody incidentObject,
      DynamicState referenceDynamicState, DynamicState incidentDynamicState,
      ContactPointPhysics contactPointPhysics, Vector impulse) {
    referenceDynamicState.setLinearVelocity(referenceDynamicState.getLinearVelocity()
        .subtract(impulse.multiply(referenceObject.getInverseMass())));
    referenceDynamicState.setAngularVelocity(referenceDynamicState.getAngularVelocity()
        - contactPointPhysics.positionFromReference.crossWithVector(impulse)
        * referenceObject.getInverseMomentOfInertia());

    incidentDynamicState.setLinearVelocity(incidentDynamicState.getLinearVelocity()
        .add(impulse.multiply(incidentObject.getInverseMass())));
    incidentDynamicState.setAngularVelocity(incidentDynamicState.getAngularVelocity()
        + contactPointPhysics.positionFromIncident.crossWithVector(impulse)
        * incidentObject.getInverseMomentOfInertia());
  }

  private static boolean canWarmStart(Collision collision, Vector referenceUnitNormal) {
    Vector cachedNormal = collision.getCachedNormal();
    return cachedNormal != null
        && cachedNormal.dot(referenceUnitNormal) > 0.5
        && collision.getCachedNormalImpulse() != 0.0;
  }

  private static void applyCachedImpulse(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject, DynamicState referenceDynamicState, DynamicState incidentDynamicState,
      StaticState incidentStaticState, StaticState referenceStaticState, Vector referenceUnitNormal) {
    if (!canWarmStart(collision, referenceUnitNormal)) {
      return;
    }

    int contactCount = collision.getCollisionManifold().size();
    if (contactCount == 0) {
      return;
    }
    Vector cachedImpulse = referenceUnitNormal.multiply(collision.getCachedNormalImpulse()
        / contactCount);
    for (Vector contactPoint : collision.getCollisionManifold().keySet()) {
      ContactPointPhysics contactPointPhysics = new ContactPointPhysics(contactPoint,
          collision.getCollisionManifold().get(contactPoint));
      setRelativePositions(contactPointPhysics, incidentStaticState, referenceStaticState);
      applyImpulse(referenceObject, incidentObject, referenceDynamicState, incidentDynamicState,
          contactPointPhysics, cachedImpulse);
    }
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
    if (referenceUnitNormal.magnitude() <= SOLVER_EPSILON
        || collision.getCollisionManifold() == null
        || collision.getCollisionManifold().isEmpty()) {
      return;
    }

    applyCachedImpulse(collision, referenceObject, incidentObject, referenceDynamicState,
        incidentDynamicState, incidentStaticState, referenceStaticState, referenceUnitNormal);

    double normalImpulseSum = collision.getCachedNormalImpulse();
    double frictionImpulseSum = 0.0;
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

      Vector normalImpulse = referenceUnitNormal.multiply(contactPointPhysics.impulse);
      applyImpulse(referenceObject, incidentObject, referenceDynamicState, incidentDynamicState,
          contactPointPhysics, normalImpulse);
      normalImpulseSum += contactPointPhysics.impulse;

      contactPointPhysics.relativeVelocity = ImpulseSolverUtilities.computeRelativeVelocity(
          contactPointPhysics,
          incidentDynamicState, referenceDynamicState);
      Vector tangent = contactPointPhysics.relativeVelocity.subtract(referenceUnitNormal.multiply(
          contactPointPhysics.relativeVelocity.dot(referenceUnitNormal))).normalize();
      double frictionImpulseMagnitude = computeFrictionImpulse(referenceObject, incidentObject,
          contactPointPhysics, referenceUnitNormal, contactPointPhysics.impulse);
      applyImpulse(referenceObject, incidentObject, referenceDynamicState, incidentDynamicState,
          contactPointPhysics, tangent.multiply(frictionImpulseMagnitude));
      frictionImpulseSum += frictionImpulseMagnitude;
    }
    collision.setAppliedNormalImpulse(Math.max(0.0, normalImpulseSum));
    collision.setAppliedFrictionImpulse(frictionImpulseSum);
  }
}
