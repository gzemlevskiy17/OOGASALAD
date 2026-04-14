package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.DistanceJoint;
import org.junit.jupiter.api.Test;

class DistanceJointTest {

  @Test
  void apply_zeroStiffnessBehavesAsRigidDistanceConstraint() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
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
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
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
  void apply_positiveStiffnessWhenCompressedVertically_pushesBodiesApart() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 4, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 8.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(0, 2));
    joint.apply(1.0);

    assertTrueEquals(0.0, objectA.getForce().x());
    assertTrueEquals(-16.0, objectA.getForce().y());
    assertTrueEquals(0.0, objectB.getForce().x());
    assertTrueEquals(16.0, objectB.getForce().y());
  }

  @Test
  void apply_positiveStiffnessWhenStretchedDiagonally_pullsBodiesTogether() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(3, 4, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 10.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(6, 8));
    joint.apply(1.0);

    assertTrueEquals(30.0, objectA.getForce().x());
    assertTrueEquals(40.0, objectA.getForce().y());
    assertTrueEquals(-30.0, objectB.getForce().x());
    assertTrueEquals(-40.0, objectB.getForce().y());
  }

  @Test
  void apply_withDistanceRangeOnlyCorrectsOutsideBand() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(4, 0, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 3.0, 5.0, 10.0, 0.0);

    joint.apply(1.0);
    assertTrueEquals(0.0, objectA.getForce().x());
    assertTrueEquals(0.0, objectB.getForce().x());

    objectB.getState().setLinearPosition(new Vector(7, 0));
    joint.apply(1.0);
    assertTrueEquals(20.0, objectA.getForce().x());
    assertTrueEquals(-20.0, objectB.getForce().x());
  }

  @Test
  void apply_zeroStiffnessCorrectsVerticalCompression() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 4, 0);
    DistanceJoint joint = new DistanceJoint(objectA, objectB, 0.0, 0.0);

    objectB.getState().setLinearPosition(new Vector(0, 2));
    joint.apply(1.0);

    assertTrueEquals(0.0, objectA.getX());
    assertTrueEquals(-1.0, objectA.getY());
    assertTrueEquals(0.0, objectB.getX());
    assertTrueEquals(3.0, objectB.getY());
  }

  @Test
  void apply_zeroStiffnessCorrectsDiagonalStretchForDynamicStaticPair() {
    TestDynamicComponent dynamic = new TestDynamicComponent(0, 0, 0);
    TestStaticComponent fixed = new TestStaticComponent(3, 4, 0);
    DistanceJoint joint = new DistanceJoint(dynamic, fixed, 0.0, 0.0);

    dynamic.getState().setLinearPosition(new Vector(-3, -4));
    joint.apply(1.0);

    assertTrueEquals(0.0, dynamic.getX());
    assertTrueEquals(0.0, dynamic.getY());
    assertTrueEquals(3.0, fixed.getX());
    assertTrueEquals(4.0, fixed.getY());
  }

  @Test
  void anchors_shiftConstraintEndpointsAwayFromBodyCenters() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, 0);
    TestDynamicComponent objectB = new TestDynamicComponent(10, 0, 0);
    DistanceJoint joint = new DistanceJoint(
        objectA,
        objectB,
        new Vector(2, 0),
        new Vector(-2, 0),
        -1.0,
        -1.0,
        10.0,
        0.0
    );

    assertTrueEquals(2.0, joint.getWorldAnchorA().x());
    assertTrueEquals(8.0, joint.getWorldAnchorB().x());

    objectB.getState().setLinearPosition(new Vector(12, 0));
    joint.apply(1.0);

    assertTrueEquals(20.0, objectA.getForce().x());
    assertTrueEquals(-20.0, objectB.getForce().x());
  }

  @Test
  void anchors_rotateWithBodiesBeforeDistanceIsMeasured() {
    TestDynamicComponent objectA = new TestDynamicComponent(0, 0, Math.PI / 2);
    TestDynamicComponent objectB = new TestDynamicComponent(0, 10, Math.PI);
    DistanceJoint joint = new DistanceJoint(
        objectA,
        objectB,
        new Vector(2, 0),
        new Vector(1, 0),
        -1.0,
        -1.0,
        5.0,
        0.0
    );

    assertTrueEquals(0.0, joint.getWorldAnchorA().x());
    assertTrueEquals(2.0, joint.getWorldAnchorA().y());
    assertTrueEquals(-1.0, joint.getWorldAnchorB().x());
    assertTrueEquals(10.0, joint.getWorldAnchorB().y());

    objectB.getState().setLinearPosition(new Vector(0, 14));
    joint.apply(1.0);

    assertTrueEquals(-1.6523296829400695, objectA.getForce().x());
    assertTrueEquals(19.827956195280834, objectA.getForce().y());
    assertTrueEquals(1.6523296829400695, objectB.getForce().x());
    assertTrueEquals(-19.827956195280834, objectB.getForce().y());
  }

  @Test
  void constructor_allowsStaticStaticAndPassthroughPairs() {
    TestStaticComponent staticA = new TestStaticComponent(0, 0, 0);
    TestStaticComponent staticB = new TestStaticComponent(2, 0, 0);
    TestDynamicComponent dynamic = new TestDynamicComponent(0, 0, 0);
    TestPassthroughComponent passthrough = new TestPassthroughComponent(2, 0, 0);
    new DistanceJoint(staticA, staticB, 0.0, 0.0);
    new DistanceJoint(dynamic, passthrough, 0.0, 0.0);
  }

  private void assertTrueEquals(double expected, double actual) {
    assertEquals(expected, actual, 1e-9);
  }

  private static class TestDynamicComponent extends DynamicComponent {
    TestDynamicComponent(double x, double y, double rotation) {
      super(x, y, rotation, 1.0, 0.5);
    }
  }

  private static class TestStaticComponent extends StaticComponent {
    TestStaticComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.5);
      setAffectedByGravity(false);
    }
  }

  private static class TestPassthroughComponent extends PassthroughComponent {
    TestPassthroughComponent(double x, double y, double rotation) {
      super(x, y, rotation, 0.0, 0.0);
    }

    @Override
    public void applyEffect(GameObject target) {
    }
  }
}
