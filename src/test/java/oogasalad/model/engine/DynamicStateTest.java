package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DynamicState} (replaces the former ObjectState type used in early drafts).
 */
class DynamicStateTest {

  private static final double EPS = 1e-9;

  @Test
  void defaultConstructor_initializesToZeroedVectorsAndScalars() {
    DynamicState s = new DynamicState();

    assertEquals(0, s.getLinearVelocity().x(), EPS);
    assertEquals(0, s.getLinearVelocity().y(), EPS);
    assertEquals(0, s.getLinearAcceleration().x(), EPS);
    assertEquals(0, s.getLinearAcceleration().y(), EPS);
    assertEquals(0, s.getAngularVelocity(), EPS);
    assertEquals(0, s.getAngularAcceleration(), EPS);
  }

  @Test
  void fullConstructor_storesAllFields() {
    Vector vel = new Vector(3.0, 4.0);
    Vector acc = new Vector(5.0, 6.0);
    DynamicState s = new DynamicState(vel, acc, 0.5, 1.0);

    assertEquals(vel, s.getLinearVelocity());
    assertEquals(acc, s.getLinearAcceleration());
    assertEquals(0.5, s.getAngularVelocity(), EPS);
    assertEquals(1.0, s.getAngularAcceleration(), EPS);
  }

  @Test
  void setLinearVelocity_roundTrips() {
    DynamicState s = new DynamicState();
    s.setLinearVelocity(new Vector(2.0, -3.0));
    assertEquals(2.0, s.getLinearVelocity().x(), EPS);
    assertEquals(-3.0, s.getLinearVelocity().y(), EPS);
  }

  @Test
  void setLinearAcceleration_roundTrips() {
    DynamicState s = new DynamicState();
    s.setLinearAcceleration(new Vector(1.0, 2.0));
    assertEquals(1.0, s.getLinearAcceleration().x(), EPS);
    assertEquals(2.0, s.getLinearAcceleration().y(), EPS);
  }

  @Test
  void setAngularVelocityAndAcceleration_roundTrip() {
    DynamicState s = new DynamicState();
    s.setAngularVelocity(3.14);
    s.setAngularAcceleration(0.2);
    assertEquals(3.14, s.getAngularVelocity(), EPS);
    assertEquals(0.2, s.getAngularAcceleration(), EPS);
  }
}
