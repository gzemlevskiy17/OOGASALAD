package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.RigidJoint;
import org.junit.jupiter.api.Test;

class RigidJointTest {

  @Test
  void apply_dynamicDynamicPairAddsOppositeConstraintForcesWithoutTeleportingBodies() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(4, 0, 0));
    RigidJoint joint = new RigidJoint(objectA, objectB, 100.0, 0.0);

    objectB.getState().setLinearPosition(new oogasalad.model.engine.api.Vector(6, 0));
    joint.apply();

    assertEquals(200.0, objectA.getForce().x(), 1e-9);
    assertEquals(-200.0, objectB.getForce().x(), 1e-9);
    assertEquals(0.0, objectA.getX(), 1e-9);
    assertEquals(6.0, objectB.getX(), 1e-9);
  }

  @Test
  void apply_dynamicStaticPairOnlyAddsForceToDynamicBody() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestStaticComponent objectB = new TestStaticComponent(new Pose(4, 0, 0));
    RigidJoint joint = new RigidJoint(objectA, objectB, 100.0, 0.0);

    objectA.getState().setLinearPosition(new oogasalad.model.engine.api.Vector(1, 0));
    joint.apply();

    assertEquals(-100.0, objectA.getForce().x(), 1e-9);
    assertEquals(1.0, objectA.getX(), 1e-9);
    assertEquals(4.0, objectB.getX(), 1e-9);
  }

  @Test
  void constructor_rejectsStaticStaticAndPassthroughPairs() {
    TestStaticComponent staticA = new TestStaticComponent(new Pose(0, 0, 0));
    TestStaticComponent staticB = new TestStaticComponent(new Pose(2, 0, 0));
    TestDynamicComponent dynamic = new TestDynamicComponent(new Pose(0, 0, 0));
    TestPassthroughComponent passthrough = new TestPassthroughComponent(new Pose(2, 0, 0));

    assertThrows(IllegalArgumentException.class, () -> new RigidJoint(staticA, staticB));
    assertThrows(IllegalArgumentException.class, () -> new RigidJoint(dynamic, passthrough));
  }

  @Test
  void constructor_exposesOriginalEndpointsThroughJointInterface() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestStaticComponent objectB = new TestStaticComponent(new Pose(4, 0, 0));
    RigidJoint joint = new RigidJoint(objectA, objectB);

    assertEquals(objectA, joint.getObjectA());
    assertEquals(objectB, joint.getObjectB());
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
