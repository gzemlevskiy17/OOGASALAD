package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.view.core.api.SingleSprite;
import oogasalad.view.core.api.Sprite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaletteSpriteRegistryTest {

  private PaletteSpriteRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new PaletteSpriteRegistry();
  }

  @Test
  void resolve_pigComponent_returnsSpriteFromPalette() {
    PigComponent pig = new PigComponent(0, 0, 0, 1, 0.3);
    Sprite sprite = registry.resolve(pig);
    assertNotNull(sprite);
    SingleSprite single = assertInstanceOf(SingleSprite.class, sprite);
    assertEquals("data/assets/vehicle/bp_bigPig.png", single.path());
    assertEquals(40.0, single.width(), 1e-9);
    assertEquals(40.0, single.height(), 1e-9);
  }

  @Test
  void resolveByType_pig_returnsSingleSprite() {
    Sprite sprite = registry.resolveByType("Pig");
    assertInstanceOf(SingleSprite.class, sprite);
  }

  @Test
  void resolveByType_smallPig_stripsSpaceAndResolves() {
    Sprite sprite = registry.resolveByType("Small Pig");
    SingleSprite single = assertInstanceOf(SingleSprite.class, sprite);
    assertEquals("data/assets/vehicle/bp_smallPig.png", single.path());
  }

  @Test
  void resolveByType_unknownType_throws() {
    assertThrows(IllegalArgumentException.class, () -> registry.resolveByType("NotARealType"));
  }
}
