package oogasalad.model.engine;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Utility methods for impulse-based collision response.
 *
 * <p><strong>Purpose:</strong> Centralizes normal/friction impulse calculation, warm starting,
 * and positional correction so collision solvers can apply consistent rigid-body response.
 *
 * <p><strong>Assumptions:</strong> Input collisions provide a non-empty manifold and a valid
 * penetration direction. Rigid body states are mutable and belong to the same simulation step.
 *
 * <p><strong>Dependencies:</strong> Uses {@link Collision}, {@link DynamicState},
 * {@link StaticState}, and {@link RigidBody} state accessors and mutators.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
 *     collision, referenceBody, incidentBody, incidentBody.getDynamicState(), referenceState);
 * ImpulseSolverUtilities.applyPositionalCorrection(collision, referenceBody, incidentBody);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class ImpulseSolverUtilities {

  /**
   * Lower bound used to treat tiny denominators or tangential speeds as zero.
   */
  private static final double SOLVER_EPSILON = 1e-9;
  /**
   * Below this approach-speed threshold, contacts are treated as perfectly inelastic (e=0).
   * Prevents resting contacts from endlessly bouncing when gravity adds a small approach velocity
   * every frame that restitution would otherwise reflect into a persistent oscillation. Set above a
   * single gravity impulse ({@code g·dt ≈ 16 px/s} at 60 Hz, g=980) but well under any real landing
   * or collision velocity.
   */
  private static final double RESTITUTION_APPROACH_THRESHOLD = 40.0;

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
   * Computes effective-mass denominator for an impulse applied along the provided direction.
   *
   * @param referenceObject     reference rigid body
   * @param incidentObject      incident rigid body
   * @param contactPointPhysics per-contact geometric data
   * @param unitDirection       impulse direction (normal or tangent)
   * @return scalar denominator used to scale impulse magnitude
   */
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

  /**
   * Computes scalar normal impulse for a single contact using restitution and effective mass.
   */
  private static double computeImpulse(RigidBody referenceObject, RigidBody incidentObject,
      ContactPointPhysics contactPointPhysics,
      Vector referenceUnitNormal) {
    double restitution = (incidentObject.getRestitution() + referenceObject.getRestitution()) / 2;
    double approachSpeed = -contactPointPhysics.relativeVelocity.dot(referenceUnitNormal);
    if (approachSpeed < RESTITUTION_APPROACH_THRESHOLD) {
      restitution = 0;
    }
    double normalVelocityChange = (1 + restitution) * approachSpeed;
    if (normalVelocityChange <= 0) {
      return 0;
    }

    double effectiveMassDenominator = computeImpulseDenominator(referenceObject, incidentObject,
        contactPointPhysics, referenceUnitNormal);
    if (effectiveMassDenominator <= SOLVER_EPSILON) {
      return 0;
    }

    return normalVelocityChange / effectiveMassDenominator;
  }

  /**
   * Computes Coulomb friction impulse magnitude clamped by normal impulse.
   *
   * @param referenceObject     reference rigid body
   * @param incidentObject      incident rigid body
   * @param contactPointPhysics contact data with current relative velocity
   * @param referenceUnitNormal collision normal direction
   * @param normalImpulse       normal impulse magnitude already applied
   * @return signed friction impulse magnitude for the tangent direction
   */
  private static double computeFrictionImpulse(RigidBody referenceObject, RigidBody incidentObject,
      ContactPointPhysics contactPointPhysics, Vector referenceUnitNormal, double normalImpulse) {
    Vector tangent = contactPointPhysics.relativeVelocity.subtract(referenceUnitNormal.multiply(
        contactPointPhysics.relativeVelocity.dot(referenceUnitNormal)));
    double tangentMagnitude = tangent.magnitude();
    if (tangentMagnitude <= SOLVER_EPSILON || normalImpulse <= 0.0) {
      return 0.0;
    }

    Vector unitTangent = tangent.multiply(1.0 / tangentMagnitude);
    double effectiveMassDenominator = computeImpulseDenominator(referenceObject, incidentObject,
        contactPointPhysics, unitTangent);
    if (effectiveMassDenominator <= SOLVER_EPSILON) {
      return 0.0;
    }

    double tangentImpulse =
        -contactPointPhysics.relativeVelocity.dot(unitTangent) / effectiveMassDenominator;
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

  /**
   * Applies an impulse at the contact point to both bodies' linear and angular velocities.
   *
   * @param referenceObject       reference rigid body
   * @param incidentObject        incident rigid body
   * @param referenceDynamicState mutable dynamic state for reference body
   * @param incidentDynamicState  mutable dynamic state for incident body
   * @param contactPointPhysics   geometric leverage data for angular updates
   * @param impulse               world-space impulse vector to apply
   */
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

  /**
   * Checks whether cached normal impulse data can be reused for warm starting.
   *
   * @param collision           collision that may contain cached impulses from a prior step
   * @param referenceUnitNormal current reference normal used this step
   * @return true when cache exists, has compatible direction, and has non-zero normal impulse
   */
  private static boolean canWarmStart(Collision collision, Vector referenceUnitNormal) {
    Vector cachedNormal = collision.getCachedNormal();
    return cachedNormal != null
        && cachedNormal.dot(referenceUnitNormal) > 0.9
        && collision.getCachedNormalImpulse() != 0.0;
  }

  /**
   * Applies the cached normal impulse at each contact before solving fresh impulses.
   *
   * @param collision             collision containing cached impulse and manifold
   * @param referenceObject       rigid body used as reference side
   * @param incidentObject        rigid body used as incident side
   * @param referenceDynamicState mutable dynamic state for the reference side
   * @param incidentDynamicState  mutable dynamic state for the incident side
   * @param incidentStaticState   static transform state for the incident side
   * @param referenceStaticState  static transform state for the reference side
   * @param referenceUnitNormal   normal direction used for this solve pass
   */
  private static void applyCachedImpulse(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject, DynamicState referenceDynamicState,
      DynamicState incidentDynamicState, StaticState incidentStaticState,
      StaticState referenceStaticState, Vector referenceUnitNormal) {

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
   * Validates that manifold and normal data are usable by impulse resolution.
   *
   * @param collision           collision to validate
   * @param referenceUnitNormal normal direction for response
   * @return true when required data is missing or degenerate
   */
  private static boolean hasInvalidCollisionData(Collision collision, Vector referenceUnitNormal) {
    return referenceUnitNormal.magnitude() <= SOLVER_EPSILON
        || collision.getCollisionManifold() == null
        || collision.getCollisionManifold().isEmpty();
  }

  /**
   * Creates per-contact physics data and populates relative positions for both bodies.
   *
   * @param contactPoint         manifold point being processed
   * @param collision            collision that owns the manifold
   * @param incidentStaticState  static state of the incident object
   * @param referenceStaticState static state of the reference object
   * @return initialized contact physics container for this point
   */
  private static ContactPointPhysics createContactPointPhysics(Vector contactPoint,
      Collision collision, StaticState incidentStaticState, StaticState referenceStaticState) {
    ContactPointPhysics contactPointPhysics = new ContactPointPhysics(contactPoint,
        collision.getCollisionManifold().get(contactPoint));
    setRelativePositions(contactPointPhysics, incidentStaticState, referenceStaticState);
    return contactPointPhysics;
  }

  /**
   * Computes and applies the normal impulse for a single contact point.
   *
   * @param contactPointPhysics   mutable per-contact physics values
   * @param referenceObject       reference rigid body
   * @param incidentObject        incident rigid body
   * @param referenceDynamicState mutable reference-side dynamic state
   * @param incidentDynamicState  mutable incident-side dynamic state
   * @param referenceUnitNormal   reference normal direction
   * @return scalar normal impulse applied at this contact
   */
  private static double applyNormalImpulseAtContact(ContactPointPhysics contactPointPhysics,
      RigidBody referenceObject, RigidBody incidentObject, DynamicState referenceDynamicState,
      DynamicState incidentDynamicState, Vector referenceUnitNormal) {
    contactPointPhysics.relativeVelocity = computeRelativeVelocity(contactPointPhysics,
        incidentDynamicState, referenceDynamicState);
    contactPointPhysics.impulse = computeImpulse(referenceObject, incidentObject,
        contactPointPhysics, referenceUnitNormal);

    Vector normalImpulse = referenceUnitNormal.multiply(contactPointPhysics.impulse);
    applyImpulse(referenceObject, incidentObject, referenceDynamicState, incidentDynamicState,
        contactPointPhysics, normalImpulse);
    return contactPointPhysics.impulse;
  }

  /**
   * Computes and applies a clamped Coulomb friction impulse for one contact.
   *
   * @param contactPointPhysics       mutable per-contact physics values
   * @param referenceObject           reference rigid body
   * @param incidentObject            incident rigid body
   * @param referenceDynamicState     mutable reference-side dynamic state
   * @param incidentDynamicState      mutable incident-side dynamic state
   * @param referenceUnitNormal       reference normal direction
   * @param currentTotalNormalImpulse normal impulse magnitude used as friction limit
   * @return scalar tangential impulse applied at this contact
   */
  private static double applyFrictionImpulseAtContact(ContactPointPhysics contactPointPhysics,
      RigidBody referenceObject, RigidBody incidentObject, DynamicState referenceDynamicState,
      DynamicState incidentDynamicState, Vector referenceUnitNormal,
      double currentTotalNormalImpulse) {

    contactPointPhysics.relativeVelocity = computeRelativeVelocity(contactPointPhysics,
        incidentDynamicState, referenceDynamicState);
    Vector tangent = contactPointPhysics.relativeVelocity.subtract(referenceUnitNormal.multiply(
        contactPointPhysics.relativeVelocity.dot(referenceUnitNormal))).normalize();

    double frictionImpulseMagnitude = computeFrictionImpulse(referenceObject, incidentObject,
        contactPointPhysics, referenceUnitNormal, currentTotalNormalImpulse);

    applyImpulse(referenceObject, incidentObject, referenceDynamicState, incidentDynamicState,
        contactPointPhysics, tangent.multiply(frictionImpulseMagnitude));
    return frictionImpulseMagnitude;
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

  /**
   * Resolves normal and friction impulses for all contacts in the manifold.
   *
   * <p><strong>Assumptions:</strong> The collision manifold and penetration normal were produced
   * by the detector for this frame. Passed dynamic states are mutable snapshots for the current
   * integration step.
   *
   * @param collision             collision data containing penetration and manifold points
   * @param referenceObject       reference rigid body for impulse sign convention
   * @param incidentObject        incident rigid body
   * @param incidentDynamicState  incident-side dynamic state to mutate
   * @param referenceDynamicState reference-side dynamic state to mutate
   */
  public static void resolveImpulseAtContactsHelper(Collision collision, RigidBody referenceObject,
      RigidBody incidentObject, DynamicState incidentDynamicState,
      DynamicState referenceDynamicState) {
    Vector referenceUnitNormal = collision.getPenetration().normalize();
    StaticState referenceStaticState = referenceObject.getStaticState();
    StaticState incidentStaticState = incidentObject.getStaticState();
    if (hasInvalidCollisionData(collision, referenceUnitNormal)) {
      return;
    }

    applyCachedImpulse(collision, referenceObject, incidentObject, referenceDynamicState,
        incidentDynamicState, incidentStaticState, referenceStaticState, referenceUnitNormal);

    double cachedPerContact;
    if (canWarmStart(collision, referenceUnitNormal)) {
      cachedPerContact = collision.getCachedNormalImpulse()
          / collision.getCollisionManifold().size();
    } else {
      cachedPerContact = 0.0;
    }

    double normalImpulseSum = collision.getCachedNormalImpulse();
    double frictionImpulseSum = 0.0;
    for (Vector contactPoint : collision.getCollisionManifold().keySet()) {
      ContactPointPhysics contactPointPhysics = createContactPointPhysics(contactPoint, collision,
          incidentStaticState, referenceStaticState);

      double normalImpulse = applyNormalImpulseAtContact(contactPointPhysics, referenceObject,
          incidentObject, referenceDynamicState, incidentDynamicState, referenceUnitNormal);
      normalImpulseSum += normalImpulse;

      double currentTotalNormalImpulse = Math.max(0.0, cachedPerContact + normalImpulse);
      frictionImpulseSum += applyFrictionImpulseAtContact(contactPointPhysics, referenceObject,
          incidentObject, referenceDynamicState, incidentDynamicState, referenceUnitNormal,
          currentTotalNormalImpulse);
    }
    collision.setAppliedNormalImpulse(Math.max(0.0, normalImpulseSum));
    collision.setAppliedFrictionImpulse(frictionImpulseSum);
  }
}
