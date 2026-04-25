package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.RecordingPickupEffect;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link PickupComponent}. Verifies single-fire semantics, deactivation on
 * consumption, and null-argument contracts.
 *
 * @author Paulina Vvedenskaya
 */
class PickupComponentTest {

  private RecordingPickupEffect effect;
  private DynamicComponent target;

  private static DynamicComponent newDynamic() {
    return new DynamicComponent(0, 0, 0, unitSquare(), "dyn", 0.0, 1.0, 1.0, 0.0, false) {
    };
  }

  private static ConvexPolygon unitSquare() {
    return new ConvexPolygon(List.of(
        new Vector(0, 0),
        new Vector(1, 0),
        new Vector(1, 1),
        new Vector(0, 1)));
  }

  @BeforeEach
  void setUp() {
    effect = new RecordingPickupEffect();
    target = newDynamic();
  }

  @Test
  void constructor_throwsOnNullEffect() {
    assertThrows(NullPointerException.class,
        () -> new PickupComponent(0, 0, 0, unitSquare(), "", "Test", null));
  }

  @Test
  void newPickup_isNotConsumed() {
    PickupComponent pickup = newPickup();

    assertFalse(pickup.isConsumed());
  }

  @Test
  void newPickup_isActive() {
    PickupComponent pickup = newPickup();

    assertTrue(pickup.isActive());
  }

  @Test
  void applyTo_invokesEffectExactlyOnce() {
    PickupComponent pickup = newPickup();

    pickup.applyTo(target);

    assertEquals(1, effect.callCount());
  }

  @Test
  void applyTo_passesGivenTargetToEffect() {
    PickupComponent pickup = newPickup();

    pickup.applyTo(target);

    assertSame(target, effect.lastTarget());
  }

  @Test
  void applyTo_marksPickupConsumed() {
    PickupComponent pickup = newPickup();

    pickup.applyTo(target);

    assertTrue(pickup.isConsumed());
  }

  @Test
  void applyTo_deactivatesPickup() {
    PickupComponent pickup = newPickup();

    pickup.applyTo(target);

    assertFalse(pickup.isActive());
  }

  @Test
  void applyTo_isIdempotentAfterConsumption() {
    PickupComponent pickup = newPickup();

    pickup.applyTo(target);
    pickup.applyTo(target);
    pickup.applyTo(newDynamic());

    assertEquals(1, effect.callCount());
  }

  @Test
  void applyTo_throwsOnNullTarget() {
    PickupComponent pickup = newPickup();

    assertThrows(NullPointerException.class, () -> pickup.applyTo(null));
  }

  @Test
  void applyTo_nullTarget_doesNotInvokeEffect() {
    PickupComponent pickup = newPickup();

    try {
      pickup.applyTo(null);
    } catch (NullPointerException ignored) {
    }

    assertEquals(0, effect.callCount());
  }

  @Test
  void applyTo_nullTarget_doesNotConsumePickup() {
    PickupComponent pickup = newPickup();

    try {
      pickup.applyTo(null);
    } catch (NullPointerException ignored) {
    }

    assertFalse(pickup.isConsumed());
  }

  private PickupComponent newPickup() {
    return new PickupComponent(0, 0, 0, unitSquare(), "", "Test", effect);
  }
}
