package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Compatibility wrapper for a rigid {@link DistanceJoint}.
 */
public class RigidJoint extends DistanceJoint {

  /**
   * Creates a rigid joint that locks the initial anchor separation.
   */
  public RigidJoint(GameComponent objectA, GameComponent objectB) {
    super(objectA, objectB, 0.0, 0.0);
  }

  /** Creates a rigid joint with an allowed distance band. */
  public RigidJoint(GameComponent objectA, GameComponent objectB, double minDistance, double maxDistance) {
    super(objectA, objectB, minDistance, maxDistance, 0.0, 0.0);
  }

  /** Creates a rigid joint with explicit local anchors and a distance band. */
  public RigidJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA, Vector localAnchorB, double minDistance, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, 0.0, 0.0);
  }
}
