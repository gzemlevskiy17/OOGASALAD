package oogasalad.model.engine.internal.joints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.internal.components.DynamicComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RigidJointTest {

  private WorldImpl world;
  private TestBody a;
  private TestBody b;
  private TestBody c;

  private static String canonicalKey(String ka, String kb) {
    return ka.compareTo(kb) < 0 ? ka + "|" + kb : kb + "|" + ka;
  }

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    a = new TestBody(0, 0);
    b = new TestBody(1, 0);
    c = new TestBody(0, 1);
  }

  @Test
  void registerRigidTriangle_threeComponents_addsThreeJoints() {
    RigidJoint.registerRigidTriangle(world, a, b, c);
    assertEquals(3, world.getJoints().size());
  }

  @Test
  void registerRigidTriangle_nullWorld_messageFromBundle() {
    NullPointerException ex =
        assertThrows(NullPointerException.class,
            () -> RigidJoint.registerRigidTriangle(null, a, b, c));
    assertEquals(EngineError.NULL_PARAMETER.format("world"), ex.getMessage());
  }

  @Test
  void registerRigidTriangle_listWrongSize_messageFromBundle() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> RigidJoint.registerRigidTriangle(world, List.of(a, b)));
    assertEquals(EngineError.RIGID_TRIANGLE_LIST_SIZE.format(2), ex.getMessage());
  }

  @Test
  void registerRigidTriangle_listOfThree_delegatesToTripleOverload() {
    RigidJoint.registerRigidTriangle(world, List.of(a, b, c));
    assertEquals(3, world.getJoints().size());
  }

  @Test
  void registerRigidTriangle_withDedup_skipsPairsAlreadyInSet() {
    Set<String> keys = new HashSet<>();
    keys.add(canonicalKey("0,0", "1,0"));
    keys.add(canonicalKey("0,0", "0,1"));

    RigidJoint.registerRigidTriangle(world, a, b, c, keys, "0,0", "1,0", "0,1");

    assertEquals(1, world.getJoints().size());
    assertEquals(b, world.getJoints().get(0).getComponentA());
    assertEquals(c, world.getJoints().get(0).getComponentB());
  }

  @Test
  void registerRigidTriangleDedup_nullKey_messageFromBundle() {
    Set<String> keys = new HashSet<>();
    NullPointerException ex = assertThrows(NullPointerException.class,
        () -> RigidJoint.registerRigidTriangle(world, a, b, c, keys, "0,0", "1,0", null));
    assertEquals(EngineError.NULL_PARAMETER.format("keyC"), ex.getMessage());
  }

  @Test
  void apply_preservesInitialRelativeRotation() {
    TestBody objectA = new TestBody(0, 0, 0.0);
    TestBody objectB = new TestBody(1, 0, 0.2);
    RigidJoint joint = new RigidJoint(objectA, objectB);

    objectB.getStaticState().setAngularPosition(0.8);
    joint.apply(1.0);

    assertEquals(0.2, objectB.rotation() - objectA.rotation(), 1e-9);
  }

  @Test
  void applyVelocity_zeroesRelativeAngularVelocity() {
    TestBody objectA = new TestBody(0, 0, 0.0);
    TestBody objectB = new TestBody(1, 0, 0.0);
    RigidJoint joint = new RigidJoint(objectA, objectB);
    objectA.getDynamicState().setAngularVelocity(-3.0);
    objectB.getDynamicState().setAngularVelocity(5.0);

    joint.applyVelocity();

    assertEquals(objectA.getDynamicState().getAngularVelocity(),
        objectB.getDynamicState().getAngularVelocity(), 1e-9);
  }

  private static final class TestBody extends DynamicComponent {

    TestBody(double x, double y) {
      this(x, y, 0.0);
    }

    TestBody(double x, double y, double rotation) {
      super(x, y, rotation, 1.0, 0.3);
    }
  }
}
