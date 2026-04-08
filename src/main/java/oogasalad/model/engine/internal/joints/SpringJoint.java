package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Compatibility wrapper for a soft {@link DistanceJoint}.
 */
public class SpringJoint extends DistanceJoint {

  private static final double DEFAULT_STIFFNESS = 20.0;
  private static final double DEFAULT_DAMPING = 2.0;

  public SpringJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, DEFAULT_STIFFNESS, DEFAULT_DAMPING);
  }

  public SpringJoint(GameComponent objectA, GameComponent objectB, double stiffness, double damping) {
    super(objectA, objectB, stiffness, damping);
    if (stiffness == 0.0) {throw new IllegalArgumentException("SpringJoint requires positive stiffness");}
  }
  public SpringJoint(GameComponent objectA, GameComponent objectB, double minDistance,
      double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, minDistance, maxDistance, stiffness, damping);
    if (stiffness == 0.0) {throw new IllegalArgumentException("SpringJoint requires positive stiffness");}
  }
  public SpringJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB, double minDistance, double maxDistance, double stiffness, double damping) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, stiffness,
        damping);
    if (stiffness == 0.0) {throw new IllegalArgumentException("SpringJoint requires positive stiffness");}
  }
}
