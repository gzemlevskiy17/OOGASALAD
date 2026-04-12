package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Compatibility wrapper for a soft {@link DistanceJoint}.
 */
public class SpringJoint extends DistanceJoint {

  private static final double DEFAULT_STIFFNESS = 20.0;
  private static final double DEFAULT_DAMPING = 2.0;

  /** Creates a spring joint with default stiffness and damping. */
  public SpringJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, DEFAULT_STIFFNESS, DEFAULT_DAMPING);
  }

  /** Creates a spring joint that uses the initial anchor spacing as its target length. */
  public SpringJoint(GameComponent objectA, GameComponent objectB, double stiffness, double damping) {
    super(objectA, objectB, stiffness, damping);
    if (stiffness == 0.0) {
      throw new IllegalArgumentException(EngineError.SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS.message());
    }
  }

  /** Creates a spring joint with an allowed distance band around the anchors. */
  public SpringJoint(GameComponent objectA, GameComponent objectB, double minDistance,
      double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, minDistance, maxDistance, stiffness, damping);
    if (stiffness == 0.0) {
      throw new IllegalArgumentException(EngineError.SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS.message());
    }
  }

  /** Creates a spring joint with explicit local anchors, range limits, and spring parameters. */
  public SpringJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB, double minDistance, double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, stiffness,
        damping);
    if (stiffness == 0.0) {
      throw new IllegalArgumentException(EngineError.SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS.message());
    }
  }
}
