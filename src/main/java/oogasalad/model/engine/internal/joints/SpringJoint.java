package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Force-based joint that pulls connected bodies toward their initial separation distance.
 *
 * <p>This implementation follows a simple spring-damper model and adds forces to dynamic
 * components, allowing the existing force accumulation pipeline to integrate the motion.
 */
public class SpringJoint extends AbstractJoint {

  private static final double DEFAULT_STIFFNESS = 20.0;
  private static final double DEFAULT_DAMPING = 2.0;

  private final double restDistance;
  private final double stiffness;
  private final double damping;

  public SpringJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, DEFAULT_STIFFNESS, DEFAULT_DAMPING);
  }

  public SpringJoint(GameComponent objectA, GameComponent objectB, double stiffness, double damping) {
    super(objectA, objectB);
    if (stiffness < 0.0 || damping < 0.0) {throw new IllegalArgumentException("Spring stiffness and damping must be non-negative");}
    this.restDistance = getComponentB().getState().getLinearPosition()
        .subtract(getComponentA().getState().getLinearPosition())
        .magnitude();
    this.stiffness = stiffness;
    this.damping = damping;
  }

  public double getRestDistance() {
    return restDistance;
  }

  public double getStiffness() {
    return stiffness;
  }

  public double getDamping() {
    return damping;
  }

  @Override
  public void apply() {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();

    Vector positionDelta = objectB.getState().getLinearPosition().subtract(objectA.getState().getLinearPosition());
    double currentDistance = positionDelta.magnitude();
    if (currentDistance == 0.0) {return;}
    Vector direction = positionDelta.normalize();
    double displacement = currentDistance - restDistance;
    Vector relativeVelocity = objectB.getState().getLinearVelocity()
        .subtract(objectA.getState().getLinearVelocity());
    double dampingForceMagnitude = relativeVelocity.dot(direction) * damping;
    double springForceMagnitude = (stiffness * displacement) + dampingForceMagnitude;
    Vector springForce = direction.multiply(springForceMagnitude);

    if (isDynamic(objectA)) {objectA.addForce(springForce);}
    if (isDynamic(objectB)) {objectB.addForce(springForce.multiply(-1));}
  }
}
