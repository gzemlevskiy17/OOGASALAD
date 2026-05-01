package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * Compatibility wrapper for a soft {@link DistanceJoint}.
 */
public class SpringJoint extends DistanceJoint {

  private static final double DEFAULT_STIFFNESS = 20.0;
  private static final double DEFAULT_DAMPING = 2.0;

  /**
   * Creates a spring joint with default stiffness and damping.
   */
  public SpringJoint(RigidBody objectA, RigidBody objectB) {
    this(objectA, objectB, DEFAULT_STIFFNESS, DEFAULT_DAMPING);
  }

  /** Creates a spring joint that uses the initial anchor spacing as its target length. */
  public SpringJoint(RigidBody objectA, RigidBody objectB, double stiffness, double damping) {
    super(objectA, objectB, stiffness, damping);
    checkStiffnessNonzero(stiffness);
  }

  /** Creates a spring joint with an allowed distance band around the anchors. */
  public SpringJoint(RigidBody objectA, RigidBody objectB, double minDistance,
      double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, minDistance, maxDistance, stiffness, damping);
    checkStiffnessNonzero(stiffness);
  }

  /** Creates a spring joint with explicit local anchors, range limits, and spring parameters. */
  public SpringJoint(RigidBody objectA, RigidBody objectB, Vector localAnchorA,
      Vector localAnchorB, double minDistance, double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, stiffness,
        damping);
    checkStiffnessNonzero(stiffness);
  }

  private static void checkStiffnessNonzero(double stiffness) {
    if (stiffness == 0.0) {
      throw new IllegalArgumentException(
          EngineError.SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS.message());}
  }
}
