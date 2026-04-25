package oogasalad.model.engine.internal.joints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import org.junit.jupiter.api.Test;

class DistanceJointTest {

  @Test
  void apply_rigidJointReducesDistanceErrorIntoSlop() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 0.0, 0.0);

    objectB.getStaticState().setLinearPosition(new Vector(6, 0));
    double before = anchorDistance(joint);
    joint.apply(1.0);

    assertTrue(anchorDistance(joint) < before);
    assertEquals(0.0, joint.getStiffness(), 1e-9);
  }

  @Test
  void apply_positiveStiffnessAppliesSpringForcesWithoutMovingBodies() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 10.0, 0.0);

    objectB.getStaticState().setLinearPosition(new Vector(6, 0));
    joint.apply(1.0);

    assertEquals(20.0, objectA.getForce().x(), 1e-9);
    assertEquals(-20.0, objectB.getForce().x(), 1e-9);
    assertEquals(0.0, objectA.getX(), 1e-9);
    assertEquals(6.0, objectB.getX(), 1e-9);
    assertFalse(joint.isRigid());
  }

  @Test
  void apply_distanceRangeOnlyAppliesSpringOutsideBand() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 3.0, 5.0, 10.0, 0.0);

    joint.apply(1.0);
    assertEquals(0.0, objectA.getForce().magnitude(), 1e-9);
    assertEquals(0.0, objectB.getForce().magnitude(), 1e-9);

    objectB.getStaticState().setLinearPosition(new Vector(7, 0));
    joint.apply(1.0);
    assertEquals(20.0, objectA.getForce().x(), 1e-9);
    assertEquals(-20.0, objectB.getForce().x(), 1e-9);
  }

  @Test
  void anchors_rotateWithBodiesBeforeDistanceIsMeasured() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, Math.PI / 2);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 10, Math.PI);
    DistanceJoint joint = new DistanceJoint(
        objectA, objectB, new Vector(2, 0), new Vector(1, 0),
        -1.0, -1.0, 5.0, 0.0);

    assertEquals(0.0, joint.getWorldAnchorA().x(), 1e-9);
    assertEquals(2.0, joint.getWorldAnchorA().y(), 1e-9);
    assertEquals(-1.0, joint.getWorldAnchorB().x(), 1e-9);
    assertEquals(10.0, joint.getWorldAnchorB().y(), 1e-9);
  }

  @Test
  void constructor_rejectsInvalidRangeAndNegativeSpringParameters() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);

    assertThrows(IllegalArgumentException.class,
        () -> new DistanceJoint(objectA, objectB, 5.0, 3.0, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class,
        () -> new DistanceJoint(objectA, objectB, -1.0, 0.0));
  }

  private double anchorDistance(DistanceJoint joint) {
    return joint.getWorldAnchorB().subtract(joint.getWorldAnchorA()).magnitude();
  }

  private static class TestDynamicComponent extends DynamicComponent {

    TestDynamicComponent(double x, double y, double rotation) {
      super(x, y, rotation, 1.0, 0.5);
    }
  }
}
