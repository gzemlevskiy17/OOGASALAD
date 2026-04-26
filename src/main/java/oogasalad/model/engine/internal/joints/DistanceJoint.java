package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Distance-preserving joint with optional softness.
 *
 * <p>When stiffness is zero, the joint behaves as a rigid distance constraint by correcting
 * position directly. When stiffness is positive, it behaves like a spring-damper connection.
 */
public class DistanceJoint extends AbstractJoint {

  private final double minDistance;
  private final double maxDistance;
  private final double stiffness;
  private final double damping;

  /**
   * Creates a distance joint that uses the initial anchor spacing as both its minimum and maximum
   * distance.
   */
  public DistanceJoint(RigidBody objectA, RigidBody objectB, double stiffness,
      double damping) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), -1.0, -1.0, stiffness, damping);
  }

  /**
   * Creates a distance joint with an allowed distance band and default center anchors.
   */
  public DistanceJoint(RigidBody objectA, RigidBody objectB, double minDistance,
      double maxDistance, double stiffness, double damping) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), minDistance, maxDistance, stiffness,
        damping);
  }

  /**
   * Creates a distance joint with explicit local anchors, range limits, and spring parameters.
   */
  public DistanceJoint(RigidBody objectA, RigidBody objectB, Vector localAnchorA,
      Vector localAnchorB, double minDistance, double maxDistance, double stiffness,
      double damping) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (stiffness < 0.0 || damping < 0.0) {
      throw new IllegalArgumentException(EngineError.JOINT_DISTANCE_PARAMS_NON_NEGATIVE.message());
    }
    double resolvedMin = minDistance < 0.0 ? getRestDistance() : minDistance;
    double resolvedMax = maxDistance < 0.0 ? getRestDistance() : maxDistance;
    if (resolvedMin > resolvedMax) {
      throw new IllegalArgumentException(EngineError.JOINT_DISTANCE_RANGE_INVALID.message());
    }
    this.minDistance = resolvedMin;
    this.maxDistance = resolvedMax;
    this.stiffness = stiffness;
    this.damping = damping;
  }

  private static Vector linearVelocity(RigidBody component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getLinearVelocity();
    }
    return new Vector(0, 0);
  }

  /**
   * Returns the joint stiffness used by spring mode.
   */
  public double getStiffness() {
    return stiffness;
  }

  /**
   * Returns the damping coefficient used by spring mode.
   */
  public double getDamping() {
    return damping;
  }

  /**
   * Returns the minimum allowed anchor separation.
   */
  public double getMinDistance() {
    return minDistance;
  }

  /**
   * Returns the maximum allowed anchor separation.
   */
  public double getMaxDistance() {
    return maxDistance;
  }

  /**
   * Returns whether this joint resolves as a rigid positional constraint.
   */
  public boolean isRigid() {
    return stiffness == 0.0;
  }

  /**
   * Solves either the rigid or spring form of the distance constraint.
   */
  @Override
  public void apply(double dt) {
    if (isRigid()) {
      solveRigid();
      return;
    }
    solveSpring(dt);
  }

  /**
   * Directly corrects body positions to restore the allowed distance.
   */
  protected void solveRigid() {
    applyDistanceCorrection(computeDistanceError(getCurrentDistance()));
  }

  @Override
  public void applyVelocity() {
    if (isRigid()) {
      applyAnchorVelocityEqualization();
    }
  }

  /**
   * Returns the signed distance error relative to the permitted range.
   */
  private double computeDistanceError(double currentDistance) {
    if (currentDistance < minDistance) {
      return currentDistance - minDistance;
    }
    if (currentDistance > maxDistance) {
      return currentDistance - maxDistance;
    }
    return 0.0;
  }

  /**
   * Applies spring and damping forces without directly correcting positions.
   */
  protected void solveSpring(double dt) {
    Vector delta = getAnchorDelta();
    double currentDistance = delta.magnitude();
    if (currentDistance == 0.0) {
      return;
    }
    Vector direction = delta.normalize();
    double displacement = computeDistanceError(currentDistance);
    if (Math.abs(displacement) < 1e-9) {
      return;
    }

    Vector relativeVelocity =
        linearVelocity(getComponentB()).subtract(linearVelocity(getComponentA()));
    double dampingForceMagnitude = relativeVelocity.dot(direction) * damping;
    double springForceMagnitude = (stiffness * displacement) + dampingForceMagnitude;
    Vector springForce = direction.multiply(springForceMagnitude);
    applyLinearForce(springForce, springForce.multiply(-1));
  }
}
