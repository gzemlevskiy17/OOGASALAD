package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import org.junit.jupiter.api.Test;

class RevoluteJointTest {

  @Test
  void apply_dynamicDynamicKeepsSharedPivotCoincident() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(3, 0, 0);
    RevoluteJoint joint = new RevoluteJoint(objectA, objectB, new Vector(2, 0), new Vector(-1, 0));

    objectB.getState().setLinearPosition(new Vector(5, 0));
    joint.apply(1.0);

    assertEquals(1.0, objectA.getX(), 1e-9);
    assertEquals(4.0, objectB.getX(), 1e-9);
    assertEquals(joint.getWorldAnchorA().x(), joint.getWorldAnchorB().x(), 1e-9);
    assertEquals(joint.getWorldAnchorA().y(), joint.getWorldAnchorB().y(), 1e-9);
  }

  @Test
  void apply_dynamicStaticUsesRotatedAnchorsAndOnlyMovesDynamicBody() {
    TestDynamicComponent dynamic = new TestDynamicComponent(2, 4, Math.PI / 2);
    TestStaticComponent fixed = new TestStaticComponent(2, 6, 0);
    RevoluteJoint joint = new RevoluteJoint(dynamic, fixed, new Vector(2, 0), new Vector(0, 0));

    dynamic.getState().setAngularPosition(0.0);
    joint.apply(1.0);

    assertEquals(0.0, dynamic.getX(), 1e-9);
    assertEquals(6.0, dynamic.getY(), 1e-9);
    assertEquals(2.0, fixed.getX(), 1e-9);
    assertEquals(6.0, fixed.getY(), 1e-9);
    assertEquals(joint.getWorldAnchorA().x(), joint.getWorldAnchorB().x(), 1e-9);
    assertEquals(joint.getWorldAnchorA().y(), joint.getWorldAnchorB().y(), 1e-9);
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
    TestPassthroughComponent passthrough = new TestPassthroughComponent(0, 0, 0);
    new RevoluteJoint(dynamic, passthrough);
  }

  private static class TestDynamicComponent extends DynamicComponent {
    TestDynamicComponent(double x, double y, double rotation) {super(x, y, rotation, 1.0, 0.5);}
  }

  private static class TestStaticComponent extends StaticComponent {
    TestStaticComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.5);
      setAffectedByGravity(false);
    }
  }

  private static class TestPassthroughComponent extends PassthroughComponent {
    TestPassthroughComponent(double x, double y, double rotation) {super(x, y, rotation, 0.0, 0.0);}

    @Override
    public void applyEffect(GameObject target) {}
  }
}
