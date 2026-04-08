package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Distance-preserving joint with optional softness.
 *
 * <p>When stiffness is zero, the joint behaves as a rigid distance constraint by correcting
 * position directly. When stiffness is positive, it behaves like a spring-damper connection.
 */
public class DistanceJoint extends AbstractJoint {

  private final double stiffness;
  private final double damping;

  public DistanceJoint(GameComponent objectA, GameComponent objectB, double stiffness, double damping) {
    super(objectA, objectB);
    if (stiffness < 0.0 || damping < 0.0) {throw new IllegalArgumentException("Distance joint stiffness and damping must be non-negative");}
    this.stiffness = stiffness;
    this.damping = damping;
  }

  public double getStiffness() {
    return stiffness;
  }

  public double getDamping() {
    return damping;
  }

  public boolean isRigid() {
    return stiffness == 0.0;
  }

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
    Vector delta = positionB.subtract(positionA);
    double currentDistance = delta.magnitude();

    if (currentDistance == 0.0) {return;}

    double distanceError = currentDistance - getRestDistance();
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

    Vector positionDelta = objectB.getState().getLinearPosition()
        .subtract(objectA.getState().getLinearPosition());
    double currentDistance = positionDelta.magnitude();
    if (currentDistance == 0.0) {return;}

    Vector direction = positionDelta.normalize();
    double displacement = currentDistance - getRestDistance();
    Vector relativeVelocity = objectB.getState().getLinearVelocity()
        .subtract(objectA.getState().getLinearVelocity());
    double dampingForceMagnitude = relativeVelocity.dot(direction) * damping;
    double springForceMagnitude = (stiffness * displacement) + dampingForceMagnitude;
    Vector springForce = direction.multiply(springForceMagnitude);

    if (isDynamic(objectA)) {objectA.addForce(springForce);}
    if (isDynamic(objectB)) {objectB.addForce(springForce.multiply(-1));}
  }
}
