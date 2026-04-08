package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.DistanceJoint;
import org.junit.jupiter.api.Test;

class DistanceJointTest {

  @Test
  void apply_zeroStiffnessBehavesAsRigidDistanceConstraint() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(4, 0, 0));
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 0.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(6, 0));
    joint.apply(1.0);

    assertTrueEquals(1.0, objectA.getX());
    assertTrueEquals(5.0, objectB.getX());
    assertTrueEquals(0.0, objectA.getForce().x());
    assertTrueEquals(0.0, objectB.getForce().x());
    assertEquals(0.0, joint.getStiffness(), 1e-9);
  }

  @Test
  void apply_positiveStiffnessBehavesAsSpring() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(4, 0, 0));
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 10.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(6, 0));
    joint.apply(1.0);

    assertTrueEquals(20.0, objectA.getForce().x());
    assertTrueEquals(-20.0, objectB.getForce().x());
    assertTrueEquals(0.0, objectA.getX());
    assertTrueEquals(6.0, objectB.getX());
    assertFalse(joint.isRigid());
  }

  @Test
  void constructor_rejectsStaticStaticAndPassthroughPairs() {
    TestStaticComponent staticA = new TestStaticComponent(new Pose(0, 0, 0));
    TestStaticComponent staticB = new TestStaticComponent(new Pose(2, 0, 0));
    TestDynamicComponent dynamic = new TestDynamicComponent(new Pose(0, 0, 0));
    TestPassthroughComponent passthrough = new TestPassthroughComponent(new Pose(2, 0, 0));

    assertThrows(IllegalArgumentException.class, () -> new DistanceJoint(staticA, staticB, 0.0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> new DistanceJoint(dynamic, passthrough, 0.0, 0.0));
  }

  private void assertTrueEquals(double expected, double actual) {
    assertEquals(expected, actual, 1e-9);
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

  private static class TestPassthroughComponent extends PassthroughComponent {
    TestPassthroughComponent(Pose pose) {
      super(pose, 0.0, 0.0);
    }

    @Override
    public void applyEffect(GameObject target) {
    }
  }
}
