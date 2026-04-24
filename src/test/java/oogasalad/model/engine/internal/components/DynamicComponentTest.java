package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.DynamicState;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DynamicComponentTest {

  private static final double EPS = 1e-9;

  /**
   * Unit square dynamic body; not affected by gravity.
   */
  private static DynamicComponent newBody(double mass, double x, double y) {
    ConvexPolygon box =
        new ConvexPolygon(
            List.of(
                new Vector(0, 0),
                new Vector(1, 0),
                new Vector(1, 1),
                new Vector(0, 1)));
    return new DynamicComponent(x, y, 0, box, "t", 0, mass, 1, 0.35, false) {
    };
  }

  static Stream<Arguments> forceMassToAcceleration() {
    return Stream.of(
        Arguments.of(1.0, 5.0, -3.0, 5.0, -3.0),
        Arguments.of(4.0, 8.0, 0.0, 2.0, 0.0),
        Arguments.of(0.5, 0.0, 2.0, 0.0, 4.0));
  }

  @Test
  void getDynamicState_returnsUsableStateWithZeroInitialKinematics() {
    DynamicComponent body = newBody(1, 0, 0);
    DynamicState s = body.getDynamicState();
    assertNotNull(s);
    assertEquals(0, s.getLinearVelocity().x(), EPS);
    assertEquals(0, s.getLinearVelocity().y(), EPS);
    assertEquals(0, s.getLinearAcceleration().x(), EPS);
    assertEquals(0, s.getLinearAcceleration().y(), EPS);
  }

  @Test
  void setForce_and_getForce_roundTrip() {
    DynamicComponent body = newBody(1, 0, 0);
    Vector f = new Vector(2.5, -1);
    body.setForce(f);
    assertSame(f, body.getForce());
  }

  @Test
  void resetForce_clearsToZero() {
    DynamicComponent body = newBody(1, 0, 0);
    body.addForce(new Vector(9, 9));
    body.resetForce();
    assertEquals(0, body.getForce().x(), EPS);
    assertEquals(0, body.getForce().y(), EPS);
  }

  @Test
  void addForce_accumulatesAfterReset() {
    DynamicComponent body = newBody(1, 0, 0);
    body.resetForce();
    body.addForce(new Vector(3, 0));
    body.addForce(new Vector(0, 5));
    assertEquals(3, body.getForce().x(), EPS);
    assertEquals(5, body.getForce().y(), EPS);
  }

  @Test
  void addForce_afterSetForceNull_throws() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setForce(null);
    assertThrows(NullPointerException.class, () -> body.addForce(new Vector(0, 0)));
  }

  @Test
  void applyForceToAcceleration_mapsNetForceOverMass() {
    DynamicComponent body = newBody(2, 0, 0);
    body.resetForce();
    body.addForce(new Vector(10, -4));
    body.applyForceToAcceleration();
    Vector a = body.getDynamicState().getLinearAcceleration();
    assertEquals(5, a.x(), EPS);
    assertEquals(-2, a.y(), EPS);
  }

  @Test
  void applyForceToAcceleration_nonPositiveMass_yieldsZeroAcceleration() {
    DynamicComponent body = newBody(0, 0, 0);
    body.resetForce();
    body.addForce(new Vector(100, 200));
    body.applyForceToAcceleration();
    Vector a = body.getDynamicState().getLinearAcceleration();
    assertEquals(0, a.x(), EPS);
    assertEquals(0, a.y(), EPS);
  }

  @Test
  void applyForceToAcceleration_nullForce_yieldsZeroAcceleration() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setForce(null);
    body.applyForceToAcceleration();
    Vector a = body.getDynamicState().getLinearAcceleration();
    assertEquals(0, a.x(), EPS);
    assertEquals(0, a.y(), EPS);
  }

  @Test
  void updateSleepState_lowVelocityLongEnough_entersSleep() {
    DynamicComponent body = newBody(1, 0, 0);

    body.updateSleepState(0.6);

    assertTrue(body.isSleeping());
  }

  @Test
  void updateSleepState_highVelocity_keepsBodyAwake() {
    DynamicComponent body = newBody(1, 0, 0);
    body.getDynamicState().setLinearVelocity(new Vector(5, 0));

    body.updateSleepState(1.0);

    assertTrue(!body.isSleeping());
  }

  @Test
  void setSleeping_trueClearsVelocityAndForces() {
    DynamicComponent body = newBody(1, 0, 0);
    body.getDynamicState().setLinearVelocity(new Vector(10, 0));
    body.addForce(new Vector(5, 0));

    body.setSleeping(true);

    assertEquals(0, body.getDynamicState().getLinearVelocity().magnitude(), EPS);
    assertEquals(0, body.getForce().magnitude(), EPS);
  }

  @Test
  void addForce_nonZeroForceWakesSleepingBody() {
    DynamicComponent body = newBody(2, 0, 0);
    body.setSleeping(true);

    body.addForce(new Vector(0, 10));
    body.applyForceToAcceleration();

    assertTrue(!body.isSleeping());
    assertEquals(5, body.getDynamicState().getLinearAcceleration().y(), EPS);
  }

  @ParameterizedTest
  @MethodSource("forceMassToAcceleration")
  void applyForceToAcceleration_parameterized(
      double mass, double fx, double fy, double ax, double ay) {
    DynamicComponent body = newBody(mass, 0, 0);
    body.resetForce();
    body.addForce(new Vector(fx, fy));
    body.applyForceToAcceleration();
    Vector a = body.getDynamicState().getLinearAcceleration();
    assertEquals(ax, a.x(), EPS);
    assertEquals(ay, a.y(), EPS);
  }

  /**
   * Integrator: v += a dt, then x += v dt using updated v. With v0 = 0: Δx = a dt².
   */
  @Test
  void integrateObjectState_constantLinearAcceleration_matchesSemiImplicitEuler() {
    DynamicComponent body = newBody(1, 7, 11);
    double dt = 0.1;
    body.resetForce();
    body.addForce(new Vector(12, 0));
    body.applyForceToAcceleration();
    body.integrateObjectState(dt);

    assertEquals(7 + 12 * dt * dt, body.getStaticState().getLinearPosition().x(), EPS);
    assertEquals(11, body.getStaticState().getLinearPosition().y(), EPS);
    assertEquals(12 * dt, body.getDynamicState().getLinearVelocity().x(), EPS);
    assertEquals(0, body.getDynamicState().getLinearVelocity().y(), EPS);
  }

  @Test
  void integrateObjectState_twoSteps_matchesHandComputedEuler() {
    DynamicComponent body = newBody(2, 0, 0);
    double dt = 0.05;
    body.resetForce();
    body.addForce(new Vector(0, -8));
    body.applyForceToAcceleration();
    double ay = -4;
    body.integrateObjectState(dt);
    body.integrateObjectState(dt);

    double vy1 = ay * dt;
    double y1 = vy1 * dt;
    double vy2 = vy1 + ay * dt;
    double y2 = y1 + vy2 * dt;

    assertEquals(y2, body.getStaticState().getLinearPosition().y(), EPS);
    assertEquals(vy2, body.getDynamicState().getLinearVelocity().y(), EPS);
  }

  @Test
  void getMass_and_setMass_roundTrip() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setMass(3.25);
    assertEquals(3.25, body.getMass(), EPS);
  }

  @Test
  void setMass_negative_throws() {
    DynamicComponent body = newBody(1, 0, 0);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> body.setMass(-1));
    assertTrue(ex.getMessage().contains(EngineError.MASS_MUST_BE_NON_NEGATIVE.message()));
  }

  @Test
  void inverseMass_zeroMass_returnsZeroInsteadOfInfinity() {
    DynamicComponent body = newBody(0, 0, 0);

    assertEquals(0.0, body.getInverseMass(), EPS);
  }

  @Test
  void affectedByGravity_roundTrip() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setAffectedByGravity(true);
    assertTrue(body.isAffectedByGravity());
  }

  @Test
  void momentOfInertia_and_restitution_roundTrip() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setMomentOfInertia(2.5);
    body.setRestitution(0.8);
    assertEquals(2.5, body.getMomentOfInertia(), EPS);
    assertEquals(0.8, body.getRestitution(), EPS);
  }

  @Test
  void inverseMomentOfInertia_zeroMoment_returnsZeroInsteadOfInfinity() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setMomentOfInertia(0.0);

    assertEquals(0.0, body.getInverseMomentOfInertia(), EPS);
  }

  @Test
  void setMomentOfInertia_negative_throws() {
    DynamicComponent body = newBody(1, 0, 0);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> body.setMomentOfInertia(-1));
    assertTrue(ex.getMessage()
        .contains(EngineError.MOMENT_OF_INERTIA_MUST_BE_NON_NEGATIVE.message()));
  }

  @Test
  void setRestitution_outOfRange_throws() {
    DynamicComponent body = newBody(1, 0, 0);

    assertThrows(IllegalArgumentException.class, () -> body.setRestitution(-0.1));
    assertThrows(IllegalArgumentException.class, () -> body.setRestitution(1.1));
  }

  @Test
  void getFriction_and_setFriction_roundTrip() {
    DynamicComponent body = newBody(1, 0, 0);
    body.setFriction(0.42);
    assertEquals(0.42, body.getFriction(), EPS);
  }

  @Test
  void setFriction_negative_throws() {
    DynamicComponent body = newBody(1, 0, 0);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> body.setFriction(-0.1));
    assertTrue(ex.getMessage().contains(EngineError.FRICTION_MUST_BE_NON_NEGATIVE.message()));
  }
}