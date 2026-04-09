package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Compatibility wrapper for a rigid {@link DistanceJoint}.
 */
public class RigidJoint extends DistanceJoint {

  public RigidJoint(GameComponent objectA, GameComponent objectB) {
    super(objectA, objectB, 0.0, 0.0);
  }

  public RigidJoint(GameComponent objectA, GameComponent objectB, double minDistance, double maxDistance) {
    super(objectA, objectB, minDistance, maxDistance, 0.0, 0.0);
  }

  public RigidJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA, Vector localAnchorB, double minDistance, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, 0.0, 0.0);
  }
}
