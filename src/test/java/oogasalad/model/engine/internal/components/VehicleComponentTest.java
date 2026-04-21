package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehicleComponentTest {

  private DriveComponent vehicle;

  @BeforeEach
  void setUp() {
    vehicle = new DriveComponent(1.0, 2.0, 45.0, 10.0, 0.3, 100.0, "TestCar", "", 64.0, 48.0);
  }

  // ── identity + body fields ────────────────────────────────────────────────

  @Test
  void getBodyWidth_returnsConstructorValue() {
    assertEquals(64.0, vehicle.getBodyWidth(), 1e-9);
  }

  @Test
  void getBodyHeight_returnsConstructorValue() {
    assertEquals(48.0, vehicle.getBodyHeight(), 1e-9);
  }

  @Test
  void getDisplayName_returnsConstructorValue() {
    assertEquals("TestCar", vehicle.getDisplayName());
  }

  @Test
  void getComponentType_matchesDisplayName() {
    assertEquals("TestCar", vehicle.getComponentType());
  }

  // ── initial HP state ──────────────────────────────────────────────────────

  @Test
  void initialCurrentHp_equalsMaxHp() {
    assertEquals(vehicle.getMaxHp(), vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void initiallyNotDestroyed() {
    assertFalse(vehicle.isDestroyed());
  }

  // ── maxHp validation ──────────────────────────────────────────────────────

  @Test
  void negativeMaxHpInConstructor_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () ->
        new DriveComponent(0, 0, 0, 1, 0, -1, "X", "", 1, 1));
  }

  @Test
  void setMaxHp_negativeValue_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> vehicle.setMaxHp(-10.0));
  }

  @Test
  void setMaxHp_validValue_updates() {
    vehicle.setMaxHp(200.0);
    assertEquals(200.0, vehicle.getMaxHp(), 1e-9);
  }

  // ── damage ────────────────────────────────────────────────────────────────

  @Test
  void damage_reducesCurrentHp() {
    vehicle.damage(30.0);
    assertEquals(70.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void damage_clampsToZero_notNegative() {
    vehicle.damage(999.0);
    assertEquals(0.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void damage_exactlyMaxHp_setsToZero() {
    vehicle.damage(100.0);
    assertEquals(0.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void damage_toZero_vehicleIsDestroyed() {
    vehicle.damage(100.0);
    assertTrue(vehicle.isDestroyed());
  }

  @Test
  void damage_belowMaxHp_notDestroyed() {
    vehicle.damage(50.0);
    assertFalse(vehicle.isDestroyed());
  }

  @Test
  void damage_negativeAmount_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> vehicle.damage(-1.0));
  }

  @Test
  void damage_zero_doesNotChangeHp() {
    vehicle.damage(0.0);
    assertEquals(100.0, vehicle.getCurrentHp(), 1e-9);
  }

  // ── repair ────────────────────────────────────────────────────────────────

  @Test
  void repair_increasesCurrentHp() {
    vehicle.damage(40.0);
    vehicle.repair(15.0);
    assertEquals(75.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void repair_clampsToMaxHp() {
    vehicle.damage(20.0);
    vehicle.repair(999.0);
    assertEquals(100.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void repair_exactlyToMaxHp_doesNotExceed() {
    vehicle.damage(50.0);
    vehicle.repair(50.0);
    assertEquals(100.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void repair_negativeAmount_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> vehicle.repair(-5.0));
  }

  @Test
  void repair_zero_doesNotChangeHp() {
    vehicle.damage(30.0);
    vehicle.repair(0.0);
    assertEquals(70.0, vehicle.getCurrentHp(), 1e-9);
  }

  // ── setCurrentHp ─────────────────────────────────────────────────────────

  @Test
  void setCurrentHp_validValue_updates() {
    vehicle.setCurrentHp(55.0);
    assertEquals(55.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void setCurrentHp_aboveMaxHp_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> vehicle.setCurrentHp(101.0));
  }

  @Test
  void setCurrentHp_negative_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> vehicle.setCurrentHp(-1.0));
  }

  @Test
  void setCurrentHp_zero_vehicleIsDestroyed() {
    vehicle.setCurrentHp(0.0);
    assertTrue(vehicle.isDestroyed());
  }

  // ── destroy ───────────────────────────────────────────────────────────────

  @Test
  void destroy_setsCurrentHpToZero() {
    vehicle.destroy();
    assertEquals(0.0, vehicle.getCurrentHp(), 1e-9);
  }

  @Test
  void destroy_vehicleIsDestroyed() {
    vehicle.destroy();
    assertTrue(vehicle.isDestroyed());
  }

  // ── isDestroyed edge cases ────────────────────────────────────────────────

  @Test
  void isDestroyed_afterPartialDamage_isFalse() {
    vehicle.damage(99.9);
    assertFalse(vehicle.isDestroyed());
  }
}
