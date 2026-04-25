package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import org.junit.jupiter.api.Test;

class RevoluteJointTest {

  @Test
  void apply_dynamicDynamicKeepsSharedPivotCoincident() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(3, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(objectA, objectB, new Vector(2, 0), new Vector(-1, 0));

    objectB.getStaticState().setLinearPosition(new Vector(5, 0));
    double initialError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
    joint.apply(1.0);

    double remainingError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
    assertTrue(remainingError < initialError);
  }

  @Test
  void apply_dynamicStaticUsesRotatedAnchorsAndOnlyMovesDynamicBody() {
    TestDynamicComponent dynamic = new TestDynamicComponent(2, 4, Math.PI / 2);
    TestStaticComponent fixed = new TestStaticComponent(2, 6, 0);
    RevoluteJoint joint = new RevoluteJoint(dynamic, fixed, new Vector(2, 0), new Vector(0, 0));

    dynamic.getStaticState().setAngularPosition(0.0);
    double initialError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
    joint.apply(1.0);

    assertEquals(2.0, fixed.getX(), 1e-9);
    assertEquals(6.0, fixed.getY(), 1e-9);
    double remainingError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
    assertTrue(remainingError < initialError);
  }

  @Test
  void apply_offCenterAnchorUsesAngularCorrection() {
    TestDynamicComponent frame = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent wheel = new TestDynamicComponent(0, 60, 0);
    RevoluteJoint joint = new RevoluteJoint(frame, wheel, new Vector(0, 60), new Vector(0, 0));

    frame.getStaticState().setAngularPosition(0.2);
    joint.apply(1.0);

    assertTrue(Math.abs(frame.getRotation()) < 0.2,
        "Off-center axle correction should rotate the frame back toward the wheel anchor");
  }

  @Test
  void apply_offCenterAnchorCorrectsMostErrorInOneSolve() {
    TestDynamicComponent frame = new TestDynamicComponent(60, 0, 0);
    TestDynamicComponent wheel = new TestDynamicComponent(0, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(wheel, frame, new Vector(0, 0), new Vector(-60, 0));

    frame.getStaticState().setLinearPosition(new Vector(60, 10));
    double initialError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
    joint.apply(1.0);
    double remainingError = joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();

    assertTrue(remainingError < initialError * 0.5,
        "Axle constraint should quickly convert anchor error into translation/rotation. initial="
            + initialError + " remaining=" + remainingError);
  }

  @Test
  void constructor_rejectsAnchorsThatDoNotInitiallyShareAPivot() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(5, 0, 0);

    assertThrows(IllegalArgumentException.class,
        () -> new RevoluteJoint(objectA, objectB, new Vector(0, 0), new Vector(0, 0)));
  }

  @Test
  void constructor_allowsPassthroughPairs() {
    TestDynamicComponent dynamic = new TestDynamicComponent(0, 0, 0);
    TestPassThroughComponent passthrough = new TestPassThroughComponent(0, 0, 0);
    new RevoluteJoint(dynamic, passthrough);
  }

  @Test
  void apply_withLimitsClampsRelativeAngle() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(objectA, objectB);
    joint.setLimits(-0.1, 0.1);

    objectB.getStaticState().setAngularPosition(0.5);
    joint.apply(1.0);

    assertEquals(0.1, objectB.getRotation() - objectA.getRotation(), 1e-9);
  }

  @Test
  void applyVelocity_withMotorDrivesRelativeAngularVelocity() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(objectA, objectB);
    joint.setMotor(2.0, 10.0);

    joint.applyVelocity();

    assertEquals(2.0, objectB.getDynamicState().getAngularVelocity()
        - objectA.getDynamicState().getAngularVelocity(), 1e-9);
  }

  @Test
  void setLimits_rejectsInvertedRange() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(objectA, objectB);

    assertThrows(IllegalArgumentException.class, () -> joint.setLimits(1.0, -1.0));
  }

  private static class TestDynamicComponent extends DynamicComponent {
    TestDynamicComponent(double x, double y, double rotation) {super(x, y, rotation, 1.0, 0.5);}
  }

  private static class TestStaticComponent extends StaticComponent {

    TestStaticComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.5);
    }
  }

  private static class TestPassThroughComponent extends PassThroughComponent {

    TestPassThroughComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.0);
    }

    @Override
    public void addForce(Vector force) {}

    @Override
    public void setRestitution(double restitution) {}

    @Override
    public double getFriction() {return 0.0;}

    @Override
    public boolean isAffectedByGravity() {return false;}

    @Override
    public boolean isTrigger() {return true;}
  }
}
