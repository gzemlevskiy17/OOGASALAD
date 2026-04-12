package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * One-sided distance constraint that only prevents connected bodies from separating too far.
 *
 * <p>The joint does nothing while the bodies remain within the maximum distance.
 */
public class RopeJoint extends AbstractJoint {

  private final double maxDistance;

  /** Creates a rope joint using each body's local origin as its anchor. */
  public RopeJoint(GameComponent objectA, GameComponent objectB, double maxDistance) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), maxDistance);
  }

  /** Creates a rope joint with explicit local anchors and a maximum allowed separation. */
  public RopeJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (maxDistance < getRestDistance()) {
      throw new IllegalArgumentException(EngineError.ROPE_JOINT_MAX_DISTANCE_TOO_SMALL.message());
    }
    this.maxDistance = maxDistance;
  }

  /** Returns the maximum allowed anchor separation. */
  public double getMaxDistance() {
    return maxDistance;
  }


  /** Corrects positions only when the anchors exceed the rope length. */
  @Override
  public void apply(double dt) {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();
    Vector positionA = objectA.getState().getLinearPosition();
    Vector positionB = objectB.getState().getLinearPosition();
    Vector delta = getAnchorDelta();
    double currentDistance = delta.magnitude();

    if (currentDistance == 0.0 || currentDistance <= maxDistance) {return;}

    double distanceError = currentDistance - maxDistance;
    Vector correctionDirection = delta.normalize();
    Vector correction = correctionDirection.multiply(distanceError);

    if (isDynamic(objectA) && isDynamic(objectB)) {
      objectA.getState().setLinearPosition(positionA.add(correction.multiply(0.5)));
      objectB.getState().setLinearPosition(positionB.subtract(correction.multiply(0.5)));
      return;
    }if (isDynamic(objectA) && isStatic(objectB)) {
      objectA.getState().setLinearPosition(positionA.add(correction));
      return;
    }if (isStatic(objectA) && isDynamic(objectB)) {
      objectB.getState().setLinearPosition(positionB.subtract(correction));
    }
  }
}
