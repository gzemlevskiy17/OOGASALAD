package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.RigidBody;

/**
 * One-sided distance constraint that only prevents connected bodies from separating too far.
 *
 * <p>The joint does nothing while the bodies remain within the maximum distance.
 */
public class RopeJoint extends AbstractJoint {

  private final double maxDistance;

  /**
   * Creates a rope joint using each body's local origin as its anchor.
   */
  public RopeJoint(RigidBody objectA, RigidBody objectB, double maxDistance) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), maxDistance);
  }

  /** Creates a rope joint with explicit local anchors and a maximum allowed separation. */
  public RopeJoint(RigidBody objectA, RigidBody objectB, Vector localAnchorA,
      Vector localAnchorB, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (maxDistance < getRestDistance()) {
      throw new IllegalArgumentException(EngineError.ROPE_JOINT_MAX_DISTANCE_TOO_SMALL.message());}
    this.maxDistance = maxDistance;
  }

  /**
   * Returns the maximum allowed anchor separation.
   */
  public double getMaxDistance() {
    return maxDistance;
  }


  /** Corrects positions only when the anchors exceed the rope length. */
  @Override
  public void apply(double dt) {
    double currentDistance = getCurrentDistance();
    if (currentDistance <= maxDistance) {
      return;
    }
    applyDistanceCorrection(currentDistance - maxDistance);
  }
}
