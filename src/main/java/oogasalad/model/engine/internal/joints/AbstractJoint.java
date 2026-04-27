package oogasalad.model.engine.internal.joints;

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

  //TODO: make fake state to get rid of
  protected static double angularVelocity(RigidBody component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getAngularVelocity();
    }
    return 0.0;
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
  protected void applyLinearCorrection(Vector correctionA, Vector correctionB) {
    double inverseMassA = objectA.getInverseMass();
    double inverseMassB = objectB.getInverseMass();
    double totalInverseMass = inverseMassA + inverseMassB;

    if (totalInverseMass == 0.0) {
      return;
    }
    if (inverseMassA > 0.0) {
      objectA.getStaticState().setLinearPosition(objectA.getStaticState().getLinearPosition()
          .add(correctionA.multiply(inverseMassA / totalInverseMass)));
    }
    if (inverseMassB > 0.0) {
      objectB.getStaticState().setLinearPosition(objectB.getStaticState().getLinearPosition()
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

    double k00 = invMA + invMB + rA.y() * rA.y() * invIA + rB.y() * rB.y() * invIB;
    double k11 = invMA + invMB + rA.x() * rA.x() * invIA + rB.x() * rB.x() * invIB;
    double k01 = -rA.x() * rA.y() * invIA - rB.x() * rB.y() * invIB;
    double det = k00 * k11 - k01 * k01;
    if (Math.abs(det) < 1e-12) {
      return;
    }
    double invDet = 1.0 / det;
    double px = -invDet * (k11 * relVel.x() - k01 * relVel.y());
    double py = -invDet * (-k01 * relVel.x() + k00 * relVel.y());
    Vector impulse = new Vector(px, py);

    if (objectA instanceof DynamicComponent dA) {
      dA.getDynamicState().setLinearVelocity(vA.subtract(impulse.multiply(invMA)));
      dA.getDynamicState().setAngularVelocity(wA - rA.crossWithVector(impulse) * invIA);
    }
    if (objectB instanceof DynamicComponent dB) {
      dB.getDynamicState().setLinearVelocity(vB.add(impulse.multiply(invMB)));
      dB.getDynamicState().setAngularVelocity(wB + rB.crossWithVector(impulse) * invIB);
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

    //TODO: deduplicate
    double k00 = invMA + invMB + rA.y() * rA.y() * invIA + rB.y() * rB.y() * invIB;
    double k11 = invMA + invMB + rA.x() * rA.x() * invIA + rB.x() * rB.x() * invIB;
    double k01 = -rA.x() * rA.y() * invIA - rB.x() * rB.y() * invIB;
    double det = k00 * k11 - k01 * k01;
    if (Math.abs(det) < 1e-12) {
      return;
    }
    double invDet = 1.0 / det;
    double px = -invDet * (k11 * constraintError.x() - k01 * constraintError.y());
    double py = -invDet * (-k01 * constraintError.x() + k00 * constraintError.y());
    Vector impulse = new Vector(px, py);

    //TODO: remove ifs
    if (objectA instanceof DynamicComponent dA) {
      dA.getStaticState().setLinearPosition(dA.getStaticState().getLinearPosition()
          .subtract(impulse.multiply(invMA)));
      dA.getStaticState().setAngularPosition(dA.rotation()
          - rA.crossWithVector(impulse) * invIA);
    }
    if (objectB instanceof DynamicComponent dB) {
      dB.getStaticState().setLinearPosition(dB.getStaticState().getLinearPosition()
          .add(impulse.multiply(invMB)));
      dB.getStaticState().setAngularPosition(dB.rotation()
          + rB.crossWithVector(impulse) * invIB);
    }
  }
}
