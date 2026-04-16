package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

class ObjectStateTest {

  private static final double EPS = 1e-9;

  // ── constructor ───────────────────────────────────────────────────────────

  @Test
  void fullConstructor_storesAllFields() {
    Vector pos = new Vector(1.0, 2.0);
    Vector vel = new Vector(3.0, 4.0);
    Vector acc = new Vector(5.0, 6.0);
    ObjectState s = new ObjectState(pos, vel, acc, 0.5, 1.0, 2.0);

    assertEquals(pos, s.getLinearPosition());
    assertEquals(vel, s.getLinearVelocity());
    assertEquals(acc, s.getLinearAcceleration());
    assertEquals(0.5, s.getAngularPosition(), EPS);
    assertEquals(1.0, s.getAngularVelocity(), EPS);
    assertEquals(2.0, s.getAngularAcceleration(), EPS);
  }

  @Test
  void staticConstructor_setsZeroVelocityAndAcceleration() {
    ObjectState s = new ObjectState(new Vector(3.0, 7.0), 1.5);

    assertEquals(0.0, s.getLinearVelocity().x(), EPS);
    assertEquals(0.0, s.getLinearVelocity().y(), EPS);
    assertEquals(0.0, s.getLinearAcceleration().x(), EPS);
    assertEquals(0.0, s.getLinearAcceleration().y(), EPS);
    assertEquals(0.0, s.getAngularVelocity(), EPS);
    assertEquals(0.0, s.getAngularAcceleration(), EPS);
    assertEquals(1.5, s.getAngularPosition(), EPS);
  }

  // ── setters round-trip ────────────────────────────────────────────────────

  @Test
  void setLinearPosition_updatesGetLinearPosition() {
    ObjectState s = new ObjectState(new Vector(0, 0), 0);
    s.setLinearPosition(new Vector(9.0, 8.0));
    assertEquals(9.0, s.getLinearPosition().x(), EPS);
    assertEquals(8.0, s.getLinearPosition().y(), EPS);
  }

  @Test
  void setLinearVelocity_updatesGetLinearVelocity() {
    ObjectState s = new ObjectState(new Vector(0, 0), 0);
    s.setLinearVelocity(new Vector(2.0, -3.0));
    assertEquals(2.0, s.getLinearVelocity().x(), EPS);
    assertEquals(-3.0, s.getLinearVelocity().y(), EPS);
  }

  @Test
  void setAngularVelocity_updatesAngularVelocity() {
    ObjectState s = new ObjectState(new Vector(0, 0), 0);
    s.setAngularVelocity(3.14);
    assertEquals(3.14, s.getAngularVelocity(), EPS);
  }

  // ── integrateObjectState ──────────────────────────────────────────────────

  @Test
  void integrate_zeroElapsedTime_nothingChanges() {
    ObjectState s = new ObjectState(
        new Vector(5.0, 10.0), new Vector(1.0, 2.0), new Vector(3.0, 4.0),
        0.5, 1.0, 2.0);

    s.integrateObjectState(0.0);

    // velocity unchanged (acc * 0 = 0)
    assertEquals(1.0, s.getLinearVelocity().x(), EPS);
    assertEquals(2.0, s.getLinearVelocity().y(), EPS);
    // position unchanged (vel * 0 = 0)
    assertEquals(5.0, s.getLinearPosition().x(), EPS);
    assertEquals(10.0, s.getLinearPosition().y(), EPS);
  }

  @Test
  void integrate_constantVelocity_positionAdvances() {
    // No acceleration, just velocity
    ObjectState s = new ObjectState(
        new Vector(0.0, 0.0), new Vector(2.0, 3.0), new Vector(0.0, 0.0),
        0, 0, 0);

    s.integrateObjectState(1.0);

    // velocity unchanged
    assertEquals(2.0, s.getLinearVelocity().x(), EPS);
    assertEquals(3.0, s.getLinearVelocity().y(), EPS);
    // position = 0 + 2*1 = 2, 0 + 3*1 = 3
    assertEquals(2.0, s.getLinearPosition().x(), EPS);
    assertEquals(3.0, s.getLinearPosition().y(), EPS);
  }

  @Test
  void integrate_constantAcceleration_velocityThenPositionUpdated() {
    // Start from rest, apply acceleration
    ObjectState s = new ObjectState(
        new Vector(0.0, 0.0), new Vector(0.0, 0.0), new Vector(1.0, 0.0),
        0, 0, 0);

    s.integrateObjectState(1.0);

    // velocity updated first: v = 0 + 1*1 = 1
    assertEquals(1.0, s.getLinearVelocity().x(), EPS);
    // position updated with NEW velocity: p = 0 + 1*1 = 1  (velocity-before-position order)
    assertEquals(1.0, s.getLinearPosition().x(), EPS);
  }

  @Test
  void integrate_velocityUpdateBeforePosition() {
    // This test verifies the specific integration order: velocity updates first,
    // then position uses the updated velocity — not the old one.
    ObjectState s = new ObjectState(
        new Vector(10.0, 0.0), new Vector(0.0, 0.0), new Vector(2.0, 0.0),
        0, 0, 0);

    s.integrateObjectState(0.5);

    // velocity after: 0 + 2*0.5 = 1.0
    assertEquals(1.0, s.getLinearVelocity().x(), EPS);
    // position uses updated velocity: 10 + 1.0*0.5 = 10.5
    assertEquals(10.5, s.getLinearPosition().x(), EPS);
  }

  @Test
  void integrate_angularMotion_velocityThenPositionUpdated() {
    ObjectState s = new ObjectState(
        new Vector(0, 0), new Vector(0, 0), new Vector(0, 0),
        0.0, 0.0, 2.0);

    s.integrateObjectState(0.5);

    // angular velocity: 0 + 2*0.5 = 1.0
    assertEquals(1.0, s.getAngularVelocity(), EPS);
    // angular position: 0 + 1.0*0.5 = 0.5
    assertEquals(0.5, s.getAngularPosition(), EPS);
  }

  @Test
  void integrate_negativeVelocity_positionDecreases() {
    ObjectState s = new ObjectState(
        new Vector(10.0, 10.0), new Vector(-2.0, -3.0), new Vector(0, 0),
        0, 0, 0);

    s.integrateObjectState(2.0);

    assertEquals(6.0, s.getLinearPosition().x(), EPS);
    assertEquals(4.0, s.getLinearPosition().y(), EPS);
  }

  @Test
  void integrate_multipleSteps_compoundCorrectly() {
    // One big step vs two half steps should give different results (that is expected for Euler)
    ObjectState singleStep = new ObjectState(
        new Vector(0.0, 0.0), new Vector(0.0, 0.0), new Vector(1.0, 0.0),
        0, 0, 0);
    ObjectState twoSteps = new ObjectState(
        new Vector(0.0, 0.0), new Vector(0.0, 0.0), new Vector(1.0, 0.0),
        0, 0, 0);

    singleStep.integrateObjectState(1.0);
    twoSteps.integrateObjectState(0.5);
    twoSteps.integrateObjectState(0.5);

    // Two steps produce a different (more accurate) position than one large step
    // — this documents the known Euler behaviour rather than testing for equality.
    double singleX = singleStep.getLinearPosition().x();
    double twoX = twoSteps.getLinearPosition().x();
    // Both should be positive and twoX >= singleX because the intermediate velocity is higher
    assertTrue(twoX >= singleX);
  }
}
