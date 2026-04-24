package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.RopeJoint;
import org.junit.jupiter.api.Test;

class RopeJointTest {

  @Test
  void apply_slackRopeDoesNotMoveBodies() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 2, 0);
    RopeJoint joint = new RopeJoint(objectA, objectB, 5.0);

    joint.apply(1.0);

    assertEquals(0.0, objectA.getY(), 1e-9);
    assertEquals(2.0, objectB.getY(), 1e-9);
  }

  @Test
  void apply_overstretchedRopeReducesDistanceTowardMaximum() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
    RopeJoint joint = new RopeJoint(objectA, objectB, 5.0);

    objectB.getStaticState().setLinearPosition(new Vector(8, 0));
    double before = distance(objectA, objectB);
    joint.apply(1.0);

    assertTrue(distance(objectA, objectB) < before);
    assertTrue(distance(objectA, objectB) <= joint.getMaxDistance() + 0.5);
  }

  @Test
  void apply_overstretchedAgainstStaticAnchorMovesOnlyDynamicBody() {
    TestDynamicComponent dynamic = new TestDynamicComponent(0, -2, 0);
    TestStaticComponent fixed = new TestStaticComponent(0, 4, 0);
    RopeJoint joint = new RopeJoint(dynamic, fixed, 6.0);

    dynamic.getStaticState().setLinearPosition(new Vector(0, -4));
    joint.apply(1.0);

    assertEquals(0.0, fixed.getX(), 1e-9);
    assertEquals(4.0, fixed.getY(), 1e-9);
    assertTrue(dynamic.getY() > -4.0);
  }

  @Test
  void anchors_rotateWithBodiesBeforeRopeDistanceIsMeasured() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, Math.PI / 2);
    TestDynamicComponent objectB = new TestDynamicComponent(6, 8, 0);
    RopeJoint joint = new RopeJoint(
        objectA, objectB, new Vector(2, 0), new Vector(-1, 0), 8.0);

    assertEquals(0.0, joint.getWorldAnchorA().x(), 1e-9);
    assertEquals(2.0, joint.getWorldAnchorA().y(), 1e-9);
    assertEquals(5.0, joint.getWorldAnchorB().x(), 1e-9);
    assertEquals(8.0, joint.getWorldAnchorB().y(), 1e-9);
  }

  @Test
  void constructor_rejectsMaximumDistanceSmallerThanInitialDistance() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestStaticComponent objectB = new TestStaticComponent(4, 0, 0);

    assertThrows(IllegalArgumentException.class, () -> new RopeJoint(objectA, objectB, 3.0));
  }

  private double distance(DynamicComponent objectA, DynamicComponent objectB) {
    return objectB.getStaticState().getLinearPosition()
        .subtract(objectA.getStaticState().getLinearPosition()).magnitude();
  }

  private static class TestDynamicComponent extends DynamicComponent {

    TestDynamicComponent(double x, double y, double rotation) {
      super(x, y, rotation, 1.0, 0.5);
    }
  }

  private static class TestStaticComponent extends StaticComponent {

    TestStaticComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.5);
    }
  }
}
