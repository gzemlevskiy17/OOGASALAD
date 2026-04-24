package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Shared base for concrete joint implementations.
 *
 * <p>Stores the two endpoint components plus shared anchor and correction logic.
 */
public abstract class AbstractJoint implements Joint {

  // Anchor violations below half a pixel are ignored so joints don't fight collision
  // drift every frame.
  protected static final double ANCHOR_POSITION_SLOP = 0.5;

  private final GameComponent objectA;
  private final GameComponent objectB;
  private final oogasalad.model.engine.api.Vector localAnchorA;
  private final oogasalad.model.engine.api.Vector localAnchorB;
  private final double restDistance;
  private boolean collideConnected;

  /**
   * Creates a joint whose anchors default to each component's local origin.
   *
   * @param objectA first connected component
   * @param objectB second connected component
   */
  protected AbstractJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new oogasalad.model.engine.api.Vector(0, 0),
        new oogasalad.model.engine.api.Vector(0, 0));
  }

  /**
   * Creates a joint with explicit local-space anchors on each endpoint.
   *
   * @param objectA      first connected component
   * @param objectB      second connected component
   * @param localAnchorA anchor relative to {@code objectA}'s local frame
   * @param localAnchorB anchor relative to {@code objectB}'s local frame
   */
  protected AbstractJoint(GameComponent objectA, GameComponent objectB,
      oogasalad.model.engine.api.Vector localAnchorA,
      oogasalad.model.engine.api.Vector localAnchorB) {
    this.objectA = objectA;
    this.objectB = objectB;
    this.localAnchorA = localAnchorA;
    this.localAnchorB = localAnchorB;
    this.restDistance = getWorldAnchorB().subtract(getWorldAnchorA()).magnitude();
    this.collideConnected = false;
  }

  @Override
  public GameComponent getComponentA() {
    return objectA;
  }

  @Override
  public GameComponent getComponentB() {
    return objectB;
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
   * Returns the anchor stored in {@code objectA}'s local coordinate system.
   */
  public oogasalad.model.engine.api.Vector getLocalAnchorA() {
    return localAnchorA;
  }

  /**
   * Returns the anchor stored in {@code objectB}'s local coordinate system.
   */
  public oogasalad.model.engine.api.Vector getLocalAnchorB() {
    return localAnchorB;
  }

  /**
   * Resolves {@code objectA}'s local anchor into world-space coordinates.
   */
  public oogasalad.model.engine.api.Vector getWorldAnchorA() {
    return objectA.getStaticState().getLinearPosition()
        .add(localAnchorA.rotate(objectA.getRotation()));
  }

  /**
   * Resolves {@code objectB}'s local anchor into world-space coordinates.
   */
  public oogasalad.model.engine.api.Vector getWorldAnchorB() {
    return objectB.getStaticState().getLinearPosition()
        .add(localAnchorB.rotate(objectB.getRotation()));
  }

  /**
   * Returns the world-space vector from anchor A to anchor B.
   */
  protected oogasalad.model.engine.api.Vector getAnchorDelta() {
    return getWorldAnchorB().subtract(getWorldAnchorA());
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
    oogasalad.model.engine.api.Vector correction =
        getAnchorDelta().normalize().multiply(excess);
    applyLinearCorrection(correction, correction.multiply(-1));
  }

  /**
   * Applies equal-and-opposite positional corrections, distributing motion by inverse mass.
   *
   * <p>Bodies with non-positive mass receive zero motion, which makes static endpoints stay fixed
   * without type-specific branching in concrete joint implementations.
   */
  protected void applyLinearCorrection(
      oogasalad.model.engine.api.Vector correctionA,
      oogasalad.model.engine.api.Vector correctionB) {
    double inverseMassA = inverseMass(objectA);
    double inverseMassB = inverseMass(objectB);
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

  //TODO: double dispatch
  /**
   * Applies equal-and-opposite linear forces to movable endpoints only.
   */
  protected void applyLinearForce(Vector forceA, Vector forceB) {
    if (inverseMass(objectA) > 0.0) {
      objectA.addForce(forceA);
    }
    if (inverseMass(objectB) > 0.0) {
      objectB.addForce(forceB);
    }
  }

  private double inverseMass(GameComponent component) {
    if (!(component instanceof DynamicComponent dynamic)) {
      return 0.0;
    }
    double m = dynamic.getMass();
    return m > 0.0 ? 1.0 / m : 0.0;
  }

  private static double inverseInertia(GameComponent component) {
    if (!(component instanceof DynamicComponent dynamic)) {
      return 0.0;
    }
    return dynamic.getInverseMomentOfInertia();
  }

  private static oogasalad.model.engine.api.Vector linearVelocity(GameComponent component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getLinearVelocity();
    }
    return new oogasalad.model.engine.api.Vector(0, 0);
  }

  private static double angularVelocity(GameComponent component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getAngularVelocity();
    }
    return 0.0;
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
    double invMA = inverseMass(objectA);
    double invMB = inverseMass(objectB);
    double invIA = inverseInertia(objectA);
    double invIB = inverseInertia(objectB);
    if (invMA + invMB == 0.0) {
      return;
    }

    oogasalad.model.engine.api.Vector rA =
        getWorldAnchorA().subtract(objectA.getStaticState().getLinearPosition());
    oogasalad.model.engine.api.Vector rB =
        getWorldAnchorB().subtract(objectB.getStaticState().getLinearPosition());

    oogasalad.model.engine.api.Vector vA = linearVelocity(objectA);
    oogasalad.model.engine.api.Vector vB = linearVelocity(objectB);
    double wA = angularVelocity(objectA);
    double wB = angularVelocity(objectB);

    oogasalad.model.engine.api.Vector vAnchorA = vA.add(rA.crossWithScalar(wA));
    oogasalad.model.engine.api.Vector vAnchorB = vB.add(rB.crossWithScalar(wB));
    oogasalad.model.engine.api.Vector relVel = vAnchorB.subtract(vAnchorA);
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
    oogasalad.model.engine.api.Vector impulse = new oogasalad.model.engine.api.Vector(px, py);

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
    double invMA = inverseMass(objectA);
    double invMB = inverseMass(objectB);
    double invIA = inverseInertia(objectA);
    double invIB = inverseInertia(objectB);
    if (invMA + invMB == 0.0) {
      return;
    }

    oogasalad.model.engine.api.Vector delta = getAnchorDelta();
    double magnitude = delta.magnitude();
    if (magnitude <= ANCHOR_POSITION_SLOP) {
      return;
    }
    oogasalad.model.engine.api.Vector constraintError = delta.multiply(
        correctionFraction * (magnitude - ANCHOR_POSITION_SLOP) / magnitude);

    oogasalad.model.engine.api.Vector rA =
        getWorldAnchorA().subtract(objectA.getStaticState().getLinearPosition());
    oogasalad.model.engine.api.Vector rB =
        getWorldAnchorB().subtract(objectB.getStaticState().getLinearPosition());

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
    oogasalad.model.engine.api.Vector impulse = new oogasalad.model.engine.api.Vector(px, py);

    if (objectA instanceof DynamicComponent dA) {
      dA.getStaticState().setLinearPosition(dA.getStaticState().getLinearPosition()
          .subtract(impulse.multiply(invMA)));
      dA.getStaticState().setAngularPosition(dA.getRotation()
          - rA.crossWithVector(impulse) * invIA);
    }
    if (objectB instanceof DynamicComponent dB) {
      dB.getStaticState().setLinearPosition(dB.getStaticState().getLinearPosition()
          .add(impulse.multiply(invMB)));
      dB.getStaticState().setAngularPosition(dB.getRotation()
          + rB.crossWithVector(impulse) * invIB);
    }
  }
}
