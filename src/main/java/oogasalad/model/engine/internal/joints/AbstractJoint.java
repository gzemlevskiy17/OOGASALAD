package oogasalad.model.engine.internal.joints;

import java.util.Optional;
import oogasalad.model.engine.StaticState;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Shared base for concrete joint implementations.
 *
 * <p>Stores the two endpoint components plus shared anchor and correction logic.
 */
public abstract class AbstractJoint implements Joint {

  // Keep joint anchors nearly coincident so support/contact impulses create torque
  // instead of letting connected bodies drift through the axle.
  protected static final double ANCHOR_POSITION_SLOP = 0.05;

  private final RigidBody objectA;
  private final RigidBody objectB;
  private final Vector localAnchorA;
  private final Vector localAnchorB;
  private final double restDistance;
  private boolean collideConnected;

  /**
   * Creates a joint whose anchors default to each component's local origin.
   *
   * @param objectA first connected component
   * @param objectB second connected component
   */
  protected AbstractJoint(RigidBody objectA, RigidBody objectB) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0));
  }

  /**
   * Creates a joint with explicit local-space anchors on each endpoint.
   *
   * @param objectA      first connected component
   * @param objectB      second connected component
   * @param localAnchorA anchor relative to {@code objectA}'s local frame
   * @param localAnchorB anchor relative to {@code objectB}'s local frame
   */
  protected AbstractJoint(RigidBody objectA, RigidBody objectB, Vector localAnchorA,
      Vector localAnchorB) {
    this.objectA = objectA;
    this.objectB = objectB;
    this.localAnchorA = localAnchorA;
    this.localAnchorB = localAnchorB;
    this.restDistance = getWorldAnchorB().subtract(getWorldAnchorA()).magnitude();
    this.collideConnected = false;
  }

  protected static Vector linearVelocity(RigidBody component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getLinearVelocity();
    }
    return new Vector(0, 0);
  }

  protected static double angularVelocity(RigidBody component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getAngularVelocity();
    }
    return 0.0;
  }

  /**
   * Solves the 2×2 effective-mass system for a point-to-point correction impulse.
   *
   * <p>Given inverse masses and inverse inertias for two bodies and the lever arms {@code rA}
   * and {@code rB} from their centers to the contact/anchor, this computes the world-space
   * impulse vector that produces the requested relative velocity change {@code relVel} at the
   * contact point. The linear system arises from combining linear and angular response terms
   * for both bodies and inverting the resulting 2×2 matrix.
   *
   * <p>Returns {@link Optional#empty()} when the effective-mass matrix is singular (det ≈ 0),
   * indicating no reliable impulse can be computed.
   *
   * @param invMA inverse mass of body A
   * @param invMB inverse mass of body B
   * @param rA world-space vector from A's center to the anchor
   * @param invIA inverse moment of inertia for body A
   * @param rB world-space vector from B's center to the anchor
   * @param invIB inverse moment of inertia for body B
   * @param relVel desired relative velocity vector at the contact point (v_B - v_A)
   * @return optional world-space impulse vector that accomplishes {@code relVel} when applied
   *     with equal-and-opposite sign to the two bodies, or empty if the system is singular
   */
  private static Optional<Vector> computeCorrectionalImpulse(double invMA, double invMB, Vector rA,
      double invIA, Vector rB,
      double invIB, Vector relVel) {
    double k00 = invMA + invMB + rA.y() * rA.y() * invIA + rB.y() * rB.y() * invIB;
    double k11 = invMA + invMB + rA.x() * rA.x() * invIA + rB.x() * rB.x() * invIB;
    double k01 = -rA.x() * rA.y() * invIA - rB.x() * rB.y() * invIB;
    double det = k00 * k11 - k01 * k01;
    if (Math.abs(det) < 1e-12) {
      return Optional.empty();
    }
    double invDet = 1.0 / det;
    double px = -invDet * (k11 * relVel.x() - k01 * relVel.y());
    double py = -invDet * (-k01 * relVel.x() + k00 * relVel.y());
    return Optional.of(new Vector(px, py));
  }

  @Override
  public RigidBody getComponentA() {
    return objectA;
  }

  @Override
  public RigidBody getComponentB() {
    return objectB;
  }

  /**
   * Returns the anchor stored in {@code objectA}'s local coordinate system.
   */
  public Vector getLocalAnchorA() {
    return localAnchorA;
  }

  /**
   * Returns the anchor stored in {@code objectB}'s local coordinate system.
   */
  public Vector getLocalAnchorB() {
    return localAnchorB;
  }

  @Override
  public boolean isCollideConnected() {
    return collideConnected;
  }

  @Override
  public void setCollideConnected(boolean collideConnected) {
    this.collideConnected = collideConnected;
  }

  /**
   * Returns the world-space vector from anchor A to anchor B.
   */
  protected Vector getAnchorDelta() {
    return getWorldAnchorB().subtract(getWorldAnchorA());
  }

  /**
   * Resolves {@code objectA}'s local anchor into world-space coordinates.
   */
  public Vector getWorldAnchorA() {
    return objectA.getStaticState().getLinearPosition()
        .add(localAnchorA.rotate(objectA.rotation()));
  }

  /**
   * Resolves {@code objectB}'s local anchor into world-space coordinates.
   */
  public Vector getWorldAnchorB() {
    return objectB.getStaticState().getLinearPosition()
        .add(localAnchorB.rotate(objectB.rotation()));
  }

  /**
   * Returns the current distance between the two resolved world anchors.
   */
  protected double getCurrentDistance() {
    return getAnchorDelta().magnitude();
  }

  /**
   * Returns the anchor-to-anchor distance captured when the joint was created.
   */
  protected double getRestDistance() {
    return restDistance;
  }

  /**
   * Resolves a scalar distance error into equal-and-opposite positional corrections along the
   * current anchor direction.
   */
  protected void applyDistanceCorrection(double distanceError) {
    double currentDistance = getCurrentDistance();
    if (currentDistance == 0.0 || Math.abs(distanceError) <= ANCHOR_POSITION_SLOP) {
      return;
    }

    double excess =
        Math.signum(distanceError) * (Math.abs(distanceError) - ANCHOR_POSITION_SLOP);
    Vector correction =
        getAnchorDelta().normalize().multiply(excess);
    applyLinearCorrection(correction, correction.multiply(-1));
  }

  /**
   * Applies equal-and-opposite positional corrections, distributing motion by inverse mass.
   *
   * <p>Bodies with non-positive mass receive zero motion, which makes static endpoints stay fixed
   * without type-specific branching in concrete joint implementations.
   */
  private void applyLinearCorrection(Vector correctionA, Vector correctionB) {
    double inverseMassA = objectA.getInverseMass();
    double inverseMassB = objectB.getInverseMass();
    double totalInverseMass = inverseMassA + inverseMassB;

    StaticState aStaticState = objectA.getStaticState();
    StaticState bStaticState = objectB.getStaticState();

    if (totalInverseMass == 0.0) {
      return;
    }
    if (inverseMassA > 0.0) {
      aStaticState.setLinearPosition(aStaticState.getLinearPosition()
          .add(correctionA.multiply(inverseMassA / totalInverseMass)));
    }
    if (inverseMassB > 0.0) {
      bStaticState.setLinearPosition(bStaticState.getLinearPosition()
          .add(correctionB.multiply(inverseMassB / totalInverseMass)));
    }
  }

  /**
   * Applies equal-and-opposite linear forces to movable endpoints only.
   */
  protected void applyLinearForce(Vector forceA, Vector forceB) {
    if (objectA.getInverseMass() > 0.0) {
      objectA.addForce(forceA);
    }
    if (objectB.getInverseMass() > 0.0) {
      objectB.addForce(forceB);
    }
  }

  /**
   * Zeroes the relative linear velocity of the two anchor points in world space via an impulse
   * pair, including the angular contribution {@code ω × r} at each anchor and updating both linear
   * and angular velocity on each side. Static sides contribute zero inverse mass/inertia so
   * impulses only affect the dynamic body, which is what we want.
   *
   * <p>Call this whenever the joint should behave rigidly at the velocity level (revolute or
   * rigid-distance). Soft/spring joints shouldn't call this — they resolve velocity via forces.
   */
  protected void applyAnchorVelocityEqualization() {
    double invMA = objectA.getInverseMass();
    double invMB = objectB.getInverseMass();
    double invIA = objectA.getInverseMomentOfInertia();
    double invIB = objectB.getInverseMomentOfInertia();
    if (invMA + invMB == 0.0) {
      return;
    }

    Vector rA =
        getWorldAnchorA().subtract(objectA.getStaticState().getLinearPosition());
    Vector rB =
        getWorldAnchorB().subtract(objectB.getStaticState().getLinearPosition());

    Vector vA = linearVelocity(objectA);
    Vector vB = linearVelocity(objectB);
    double wA = angularVelocity(objectA);
    double wB = angularVelocity(objectB);

    Vector vAnchorA = vA.add(rA.crossWithScalar(wA));
    Vector vAnchorB = vB.add(rB.crossWithScalar(wB));
    Vector relVel = vAnchorB.subtract(vAnchorA);
    if (relVel.magnitude() < 1e-9) {
      return;
    }

    Optional<Vector> impulse = computeCorrectionalImpulse(invMA, invMB, rA, invIA, rB, invIB,
        relVel);

    if (impulse.isPresent()) {
      Vector correctionalImpulse = impulse.get();
      if (objectA instanceof DynamicComponent dA) {
        dA.getDynamicState().setLinearVelocity(vA.subtract(correctionalImpulse.multiply(invMA)));
        dA.getDynamicState()
            .setAngularVelocity(wA - rA.crossWithVector(correctionalImpulse) * invIA);
      }
      if (objectB instanceof DynamicComponent dB) {
        dB.getDynamicState().setLinearVelocity(vB.add(correctionalImpulse.multiply(invMB)));
        dB.getDynamicState()
            .setAngularVelocity(wB + rB.crossWithVector(correctionalImpulse) * invIB);
      }
    }
  }

  /**
   * Corrects the world-anchor separation using the same point-to-point effective mass as the
   * velocity solve, so off-center anchors may rotate bodies instead of only translating them.
   */
  protected void applyAnchorPositionCorrection(double correctionFraction) {
    double invMA = objectA.getInverseMass();
    double invMB = objectB.getInverseMass();
    double invIA = objectA.getInverseMomentOfInertia();
    double invIB = objectB.getInverseMomentOfInertia();
    if (invMA + invMB == 0.0) {
      return;
    }

    Vector delta = getAnchorDelta();
    double magnitude = delta.magnitude();
    if (magnitude <= ANCHOR_POSITION_SLOP) {
      return;
    }
    Vector constraintError = delta.multiply(
        correctionFraction * (magnitude - ANCHOR_POSITION_SLOP) / magnitude);

    Vector rA =
        getWorldAnchorA().subtract(objectA.getStaticState().getLinearPosition());
    Vector rB =
        getWorldAnchorB().subtract(objectB.getStaticState().getLinearPosition());

    Optional<Vector> impulse = computeCorrectionalImpulse(invMA, invMB, rA, invIA, rB,
        invIB, constraintError);

    if (impulse.isPresent()) {
      Vector correctionalImpulse = impulse.get();
      if (objectA instanceof DynamicComponent) {
        anchorPositionalCorrectionHelper(objectA, correctionalImpulse.multiply(-1), rA);
      }
      if (objectB instanceof DynamicComponent) {
        anchorPositionalCorrectionHelper(objectB, correctionalImpulse, rB);
      }
    }
  }

  /**
   * Applies a single body's share of an anchor-based positional correction.
   *
   * <p><strong>Purpose:</strong> Reuses the same position and rotation update pattern for callers
   * that already solved a correction impulse for the joint.
   *
   * <p><strong>Assumptions:</strong> {@code correctionalImpulse} is the signed correction vector
   * for {@code body}, and {@code relativePosition} is the world-space offset from the body center
   * to the anchor point.
   *
   * @param body body to update
   * @param correctionalImpulse correction vector for that body
   * @param relativePosition world-space anchor offset from the body center
   */
  protected void anchorPositionalCorrectionHelper(RigidBody body, Vector correctionalImpulse,
      Vector relativePosition) {
    body.getStaticState().setLinearPosition(body.getStaticState().getLinearPosition()
        .add(correctionalImpulse.multiply(body.getInverseMass())));
    body.getStaticState().setAngularPosition(body.rotation()
        + relativePosition.crossWithVector(correctionalImpulse)
        * body.getInverseMomentOfInertia());
  }

  /**
   * Applies angular correction when connected bodies drift out of alignment.
   *
   * <p><strong>Purpose:</strong> Distributes signed angular error by inverse moment of inertia so
   * the joint can restore relative orientation without moving bodies that cannot rotate.
   *
   * <p><strong>Assumptions:</strong> {@code error} is the signed angular difference in radians,
   * and {@code ANGULAR_SLOP} is the tolerance below which no correction is needed.
   *
   * @param error signed angular error to correct
   * @param ANGULAR_SLOP angular tolerance for skipping tiny corrections
   */
  protected void angularHelper(double error, double ANGULAR_SLOP) {
    if (Math.abs(error) <= ANGULAR_SLOP) {
      return;
    }

    double inverseInertiaA = getComponentA().getInverseMomentOfInertia();
    double inverseInertiaB = getComponentB().getInverseMomentOfInertia();
    double totalInverseInertia = inverseInertiaA + inverseInertiaB;
    if (totalInverseInertia == 0.0) {
      return;
    }
    if (getComponentA() instanceof DynamicComponent) {
      getComponentA().getStaticState().setAngularPosition(getComponentA().rotation()
          + error * inverseInertiaA / totalInverseInertia);
    }
    if (getComponentB() instanceof DynamicComponent) {
      getComponentB().getStaticState().setAngularPosition(getComponentB().rotation()
          - error * inverseInertiaB / totalInverseInertia);
    }
  }
}
