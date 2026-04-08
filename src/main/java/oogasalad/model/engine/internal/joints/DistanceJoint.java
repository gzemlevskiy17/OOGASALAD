package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

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

  public DistanceJoint(GameComponent objectA, GameComponent objectB, double stiffness, double damping) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), -1.0, -1.0, stiffness, damping);
  }

  public DistanceJoint(GameComponent objectA, GameComponent objectB,
      double minDistance, double maxDistance, double stiffness, double damping) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0), minDistance, maxDistance,
        stiffness, damping);
  }

  public DistanceJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
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

  public double getStiffness() {return stiffness;}
  public double getDamping() {return damping;}
  public double getMinDistance() {return minDistance;}
  public double getMaxDistance() {return maxDistance;}
  public boolean isRigid() {return stiffness == 0.0;}

  @Override
  public void apply(double dt) {
    if (isRigid()) {solveRigid();
      return;
    }solveSpring(dt);
  }

  protected void solveRigid() {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();
    Vector positionA = objectA.getState().getLinearPosition();
    Vector positionB = objectB.getState().getLinearPosition();
    Vector delta = getAnchorDelta();
    double currentDistance = delta.magnitude();

    if (currentDistance == 0.0) {return;}
    double distanceError = computeDistanceError(currentDistance);
    if (Math.abs(distanceError) < 1e-9) {return;}

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

  protected void solveSpring(double dt) {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();

    Vector positionDelta = getAnchorDelta();
    double currentDistance = positionDelta.magnitude();
    if (currentDistance == 0.0) {return;}

    Vector direction = positionDelta.normalize();
    double displacement = computeDistanceError(currentDistance);
    if (Math.abs(displacement) < 1e-9) {return;}
    Vector relativeVelocity = objectB.getState().getLinearVelocity()
        .subtract(objectA.getState().getLinearVelocity());
    double dampingForceMagnitude = relativeVelocity.dot(direction) * damping;
    double springForceMagnitude = (stiffness * displacement) + dampingForceMagnitude;
    Vector springForce = direction.multiply(springForceMagnitude);

    if (isDynamic(objectA)) {objectA.addForce(springForce);}
    if (isDynamic(objectB)) {objectB.addForce(springForce.multiply(-1));}
  }

  private double computeDistanceError(double currentDistance) {
    if (currentDistance < minDistance) {return currentDistance - minDistance;}
    if (currentDistance > maxDistance) {return currentDistance - maxDistance;}
    return 0.0;
  }
}
