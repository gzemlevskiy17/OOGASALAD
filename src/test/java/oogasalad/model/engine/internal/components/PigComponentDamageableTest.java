package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Damageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Damageable contract tests for {@link PigComponent}: construction validation, range checks,
 * damage/repair clamping, and absence of auto-deactivation at zero HP. One assertion per test.
 *
 * @author Paulina Vvedenskaya
 */
class PigComponentDamageableTest {

  private static final double EPS = 1e-9;
  private static final double SIZE = 40.0;
  private static final double MASS = 2.0;
  private static final double MAX_HP = 100.0;

  private PigComponent pig;

  private static PigComponent newPig(double maxHp) {
    return new PigComponent(
        0, 0, 0,
        ConvexPolygon.axisAlignedBox(0, 0, SIZE, SIZE),
        "",
        0.3,
        MASS,
        MASS * (SIZE * SIZE + SIZE * SIZE) / 12.0,
        0.5,
        true,
        maxHp,
        "Pig");
  }

  @BeforeEach
  void setUp() {
    pig = newPig(MAX_HP);
  }

  @Test
  void pig_isDamageable() {
    assertInstanceOf(Damageable.class, pig);
  }

  @Test
  void constructor_negativeMaxHp_throws() {
    assertThrows(IllegalArgumentException.class, () -> newPig(-1.0));
  }

  @Test
  void newPig_currentHpEqualsMaxHp() {
    assertEquals(MAX_HP, pig.getCurrentHp(), EPS);
  }

  @Test
  void newPig_getMaxHpReturnsConstructorArgument() {
    assertEquals(MAX_HP, pig.getMaxHp(), EPS);
  }

  @Test
  void setCurrentHp_validValue_updatesField() {
    pig.setCurrentHp(40.0);

    assertEquals(40.0, pig.getCurrentHp(), EPS);
  }

  @Test
  void setCurrentHp_negative_throws() {
    assertThrows(IllegalArgumentException.class, () -> pig.setCurrentHp(-1.0));
  }

  @Test
  void setCurrentHp_aboveMax_throws() {
    assertThrows(IllegalArgumentException.class, () -> pig.setCurrentHp(MAX_HP + 1.0));
  }

  @Test
  void setMaxHp_negative_throws() {
    assertThrows(IllegalArgumentException.class, () -> pig.setMaxHp(-1.0));
  }

  @Test
  void setMaxHp_validValue_updatesField() {
    pig.setMaxHp(200.0);

    assertEquals(200.0, pig.getMaxHp(), EPS);
  }

  @Test
  void damage_reducesCurrentHp() {
    pig.damage(30.0);

    assertEquals(MAX_HP - 30.0, pig.getCurrentHp(), EPS);
  }

  @Test
  void damage_amountExceedsCurrent_clampsAtZero() {
    pig.damage(MAX_HP * 10);

    assertEquals(0.0, pig.getCurrentHp(), EPS);
  }

  @Test
  void damage_negativeAmount_throws() {
    assertThrows(IllegalArgumentException.class, () -> pig.damage(-1.0));
  }

  @Test
  void damage_zeroAmount_currentHpUnchanged() {
    pig.damage(0.0);

    assertEquals(MAX_HP, pig.getCurrentHp(), EPS);
  }

  @Test
  void repair_increasesCurrentHp() {
    pig.damage(50.0);
    pig.repair(20.0);

    assertEquals(MAX_HP - 50.0 + 20.0, pig.getCurrentHp(), EPS);
  }

  @Test
  void repair_amountExceedsMax_clampsAtMax() {
    pig.damage(20.0);
    pig.repair(MAX_HP * 10);

    assertEquals(MAX_HP, pig.getCurrentHp(), EPS);
  }

  @Test
  void repair_negativeAmount_throws() {
    assertThrows(IllegalArgumentException.class, () -> pig.repair(-1.0));
  }

  @Test
  void isDestroyed_atFullHp_isFalse() {
    assertFalse(pig.isDestroyed());
  }

  @Test
  void isDestroyed_atZeroHp_isTrue() {
    pig.damage(MAX_HP);

    assertTrue(pig.isDestroyed());
  }

  @Test
  void damageToZero_doesNotDeactivatePig() {
    pig.damage(MAX_HP);

    assertTrue(pig.isActive());
  }
}
