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
