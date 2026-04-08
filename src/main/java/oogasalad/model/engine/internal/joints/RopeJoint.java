package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * One-sided distance constraint that only prevents connected bodies from separating too far.
 *
 * <p>The joint does nothing while the bodies remain within the maximum distance.
 */
public class RopeJoint extends AbstractJoint {

  private final double maxDistance;

  public RopeJoint(GameComponent objectA, GameComponent objectB, double maxDistance) {
    super(objectA, objectB);
    if (maxDistance < getRestDistance()) {
      throw new IllegalArgumentException("Rope max distance cannot be less than initial distance");
    }
    this.maxDistance = maxDistance;
  }

  public double getMaxDistance() {
    return maxDistance;
  }

  @Override
  public void apply(double dt) {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();
    Vector positionA = objectA.getState().getLinearPosition();
    Vector positionB = objectB.getState().getLinearPosition();
    Vector delta = positionB.subtract(positionA);
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
