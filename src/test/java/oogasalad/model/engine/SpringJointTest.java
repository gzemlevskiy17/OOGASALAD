package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Pose;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.SpringJoint;
import org.junit.jupiter.api.Test;

class SpringJointTest {

  @Test
  void apply_dynamicDynamicPairAddsOppositeForcesWithoutTeleportingBodies() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestDynamicComponent objectB = new TestDynamicComponent(new Pose(4, 0, 0));
    SpringJoint joint = new SpringJoint(objectA, objectB, 10.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(6, 0));
    joint.apply();

    assertEquals(20.0, objectA.getForce().x(), 1e-9);
    assertEquals(0.0, objectA.getForce().y(), 1e-9);
    assertEquals(-20.0, objectB.getForce().x(), 1e-9);
    assertEquals(0.0, objectB.getForce().y(), 1e-9);
    assertEquals(0.0, objectA.getX(), 1e-9);
    assertEquals(6.0, objectB.getX(), 1e-9);
  }

  @Test
  void apply_dynamicStaticPairOnlyAddsForceToDynamicBody() {
    TestDynamicComponent objectA = new TestDynamicComponent(new Pose(0, 0, 0));
    TestStaticComponent objectB = new TestStaticComponent(new Pose(4, 0, 0));
    SpringJoint joint = new SpringJoint(objectA, objectB, 10.0, 0.0);

    objectA.getState().setLinearPosition(new Vector(1, 0));
    joint.apply();

    assertEquals(-10.0, objectA.getForce().x(), 1e-9);
    assertEquals(0.0, objectA.getForce().y(), 1e-9);
    assertEquals(0.0, objectB.getForce().x(), 1e-9);
    assertEquals(1.0, objectA.getX(), 1e-9);
    assertEquals(4.0, objectB.getX(), 1e-9);
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
