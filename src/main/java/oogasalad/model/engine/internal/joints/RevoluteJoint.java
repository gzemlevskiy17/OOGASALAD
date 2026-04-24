package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Hinge-style joint that keeps two local anchors coincident while allowing free relative rotation.
 */
public class RevoluteJoint extends AbstractJoint {

  private static final double VECTOR_CORRECTION = 0.2;
  private static final double ANGULAR_SLOP = 1e-6;

  private final double referenceAngle;
  private boolean limitEnabled;
  private double lowerAngle;
  private double upperAngle;
  private boolean motorEnabled;
  private double motorSpeed;
  private double maxMotorTorque;

  /**
   * Creates a revolute joint using each body's local origin as the hinge anchor.
   */
  public RevoluteJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0));
  }

  /**
   * Creates a revolute joint whose two local anchors must resolve to the same world-space point.
   */
  public RevoluteJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (getAnchorDelta().magnitude() > 1e-9) {
      throw new IllegalArgumentException(
          EngineError.REVOLUTE_JOINT_REQUIRES_SHARED_ANCHOR.message());
    }
    this.referenceAngle = objectB.getRotation() - objectA.getRotation();
    this.limitEnabled = false;
    this.lowerAngle = 0.0;
    this.upperAngle = 0.0;
    this.motorEnabled = false;
    this.motorSpeed = 0.0;
    this.maxMotorTorque = 0.0;
  }

  public void setLimits(double lowerAngle, double upperAngle) {
    if (lowerAngle > upperAngle) {
      throw new IllegalArgumentException(EngineError.JOINT_ANGLE_RANGE_INVALID.message());
    }
    this.lowerAngle = lowerAngle;
    this.upperAngle = upperAngle;
    this.limitEnabled = true;
  }

  public void disableLimits() {
    this.limitEnabled = false;
  }

  public void setMotor(double motorSpeed, double maxMotorTorque) {
    if (maxMotorTorque < 0.0) {
      throw new IllegalArgumentException(EngineError.JOINT_MOTOR_PARAMS_NON_NEGATIVE.message());
    }
    this.motorSpeed = motorSpeed;
    this.maxMotorTorque = maxMotorTorque;
    this.motorEnabled = maxMotorTorque > 0.0;
  }

  /** Re-aligns the two anchors while leaving relative rotation unconstrained. */
  @Override
  public void apply(double dt) {
    Vector delta = getAnchorDelta();
    double magnitude = delta.magnitude();
    if (magnitude <= ANCHOR_POSITION_SLOP) {
      solveAngularLimit();
      return;
    }
    applyAnchorPositionCorrection(VECTOR_CORRECTION);
    solveAngularLimit();
  }

  @Override
  public void applyVelocity() {
    applyAnchorVelocityEqualization();
    applyMotorImpulse();
  }

  private double jointAngle() {
    return (getComponentB().getRotation() - getComponentA().getRotation()) - referenceAngle;
  }

  private void solveAngularLimit() {
    if (!limitEnabled) {
      return;
    }
    double angle = jointAngle();
    double error = 0.0;
    if (angle < lowerAngle) {
      error = angle - lowerAngle;
    } else if (angle > upperAngle) {
      error = angle - upperAngle;
    }
    if (Math.abs(error) <= ANGULAR_SLOP) {
      return;
    }

    double inverseInertiaA = inverseInertia(getComponentA());
    double inverseInertiaB = inverseInertia(getComponentB());
    double totalInverseInertia = inverseInertiaA + inverseInertiaB;
    if (totalInverseInertia == 0.0) {
      return;
    }
    if (getComponentA() instanceof DynamicComponent) {
      getComponentA().getStaticState().setAngularPosition(getComponentA().getRotation()
          + error * inverseInertiaA / totalInverseInertia);
    }
    if (getComponentB() instanceof DynamicComponent) {
      getComponentB().getStaticState().setAngularPosition(getComponentB().getRotation()
          - error * inverseInertiaB / totalInverseInertia);
    }
  }

  private void applyMotorImpulse() {
    if (!motorEnabled) {
      return;
    }
    double inverseInertiaA = inverseInertia(getComponentA());
    double inverseInertiaB = inverseInertia(getComponentB());
    double totalInverseInertia = inverseInertiaA + inverseInertiaB;
    if (totalInverseInertia == 0.0) {
      return;
    }
    double relativeAngularVelocity = angularVelocity(getComponentB()) - angularVelocity(
        getComponentA());
    double desiredImpulse = (relativeAngularVelocity - motorSpeed) / totalInverseInertia;
    double maxImpulse = maxMotorTorque;
    double impulse = Math.max(-maxImpulse, Math.min(desiredImpulse, maxImpulse));
    if (getComponentA() instanceof DynamicComponent dynamicA) {
      dynamicA.getDynamicState().setAngularVelocity(dynamicA.getDynamicState().getAngularVelocity()
          + impulse * inverseInertiaA);
    }
    if (getComponentB() instanceof DynamicComponent dynamicB) {
      dynamicB.getDynamicState().setAngularVelocity(dynamicB.getDynamicState().getAngularVelocity()
          - impulse * inverseInertiaB);
    }
  }

  private static double inverseInertia(GameComponent component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getInverseMomentOfInertia();
    }
    return 0.0;
  }

  private static double angularVelocity(GameComponent component) {
    if (component instanceof DynamicComponent dynamic) {
      return dynamic.getDynamicState().getAngularVelocity();
    }
    return 0.0;
  }
}
