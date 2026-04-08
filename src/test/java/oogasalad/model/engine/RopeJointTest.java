package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.RopeJoint;
import org.junit.jupiter.api.Test;

class RopeJointTest {

  @Test
  void apply_onlyCorrectsWhenBodiesExceedMaximumDistance() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(4, 0, 0));
    RopeJoint joint = new RopeJoint(objectA, objectB, 5.0);

    joint.apply(1.0);
    assertEquals(0.0, objectA.getX(), 1e-9);
    assertEquals(4.0, objectB.getX(), 1e-9);

    objectB.getState().setLinearPosition(new Vector(8, 0));
    joint.apply(1.0);

    assertEquals(1.5, objectA.getX(), 1e-9);
    assertEquals(6.5, objectB.getX(), 1e-9);
  }

  @Test
  void apply_atExactMaximumDistance_keepsRopeSlackButUnchanged() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(3, 4, 0));
    RopeJoint joint = new RopeJoint(objectA, objectB, 5.0);

    joint.apply(1.0);

    assertEquals(0.0, objectA.getX(), 1e-9);
    assertEquals(0.0, objectA.getY(), 1e-9);
    assertEquals(3.0, objectB.getX(), 1e-9);
    assertEquals(4.0, objectB.getY(), 1e-9);
  }

  @Test
  void apply_whenRopeIsFullySlack_doesNotPushBodiesApart() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(0, 2, 0));
    RopeJoint joint = new RopeJoint(objectA, objectB, 5.0);

    joint.apply(1.0);

    assertEquals(0.0, objectA.getY(), 1e-9);
    assertEquals(2.0, objectB.getY(), 1e-9);
  }

  @Test
  void apply_verticalOverstretch_onlyMovesDynamicBodyAgainstStaticAnchor() {
    TestDynamicComponent dynamic = new TestDynamicComponent(new Pose(0, 0, 0));
    TestStaticComponent fixed = new TestStaticComponent(new Pose(0, 4, 0));
    RopeJoint joint = new RopeJoint(dynamic, fixed, 4.0);

    dynamic.getState().setLinearPosition(new Vector(0, -2));
    joint.apply(1.0);

    assertEquals(0.0, dynamic.getX(), 1e-9);
    assertEquals(0.0, dynamic.getY(), 1e-9);
    assertEquals(0.0, fixed.getX(), 1e-9);
    assertEquals(4.0, fixed.getY(), 1e-9);
  }

  @Test
  void apply_diagonalOverstretchWithRotatedAnchors_correctsAlongAnchorDirection() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, Math.PI / 2));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(6, 8, 0));
    RopeJoint joint = new RopeJoint(
        objectA,
        objectB,
        new Vector(2, 0),
        new Vector(-1, 0),
        8.0
    );

    objectB.getState().setLinearPosition(new Vector(9, 12));
    joint.apply(1.0);

    assertEquals(1.5012198097823028, objectA.getX(), 1e-9);
    assertEquals(1.8765247622278787, objectA.getY(), 1e-9);
    assertEquals(7.498780190217697, objectB.getX(), 1e-9);
    assertEquals(10.123475237772121, objectB.getY(), 1e-9);
  }

  @Test
  void constructor_rejectsMaximumDistanceSmallerThanInitialDistance() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestStaticComponent objectB = new TestStaticComponent(new Pose(4, 0, 0));

    assertThrows(IllegalArgumentException.class, () -> new RopeJoint(objectA, objectB, 3.0));
  }

  private static class TestDynamicComponent extends DynamicComponent {
    TestDynamicComponent(Pose pose) {
      super(pose, 1.0, 0.5);
    }
  }

  private static class TestStaticComponent extends StaticComponent {
    TestStaticComponent(Pose pose) {
      super(pose, 0.0, 0.5);
      setAffectedByGravity(false);
    }
  }
}
