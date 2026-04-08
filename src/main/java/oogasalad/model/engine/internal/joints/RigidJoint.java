package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Compatibility wrapper for a rigid {@link DistanceJoint}.
 */
public class RigidJoint extends DistanceJoint {

  public RigidJoint(GameComponent objectA, GameComponent objectB) {
    super(objectA, objectB, 0.0, 0.0);
  }
}
