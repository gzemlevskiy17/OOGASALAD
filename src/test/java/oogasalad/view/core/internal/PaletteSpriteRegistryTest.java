package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.engine.ConvexPolygon;
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
    double w = 40.0;
    double h = 40.0;
    double mass = 1.0;
    PigComponent pig = new PigComponent(
        0,
        0,
        0,
        ConvexPolygon.axisAlignedBox(0, 0, w, h),
        "",
        0.3,
        mass,
        mass * (w * w + h * h) / 12.0,
        0.5,
        true,
        100.0,
        "Pig");
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

  @Test
  void resolveByType_bomb_returnsSingleSpriteAtBombAsset() {
    SingleSprite sprite = assertInstanceOf(SingleSprite.class, registry.resolveByType("Bomb"));
    assertEquals("data/assets/bp_bomb.png", sprite.path());
  }

  @Test
  void resolveByType_coin_returnsSingleSpriteAtCoinAsset() {
    SingleSprite sprite = assertInstanceOf(SingleSprite.class, registry.resolveByType("Coin"));
    assertEquals("data/assets/bp_coin.png", sprite.path());
  }

  @Test
  void resolveByType_heart_returnsSingleSpriteAtHeartAsset() {
    SingleSprite sprite = assertInstanceOf(SingleSprite.class, registry.resolveByType("Heart"));
    assertEquals("data/assets/bp_heart.png", sprite.path());
  }

  @Test
  void resolveByType_rock_returnsSingleSpriteAtRockAsset() {
    SingleSprite sprite = assertInstanceOf(SingleSprite.class, registry.resolveByType("Rock"));
    assertEquals("data/assets/bp_rock.png", sprite.path());
  }

  @Test
  void resolveByType_xpBoost_stripsSpaceAndResolvesToXpAsset() {
    SingleSprite sprite = assertInstanceOf(SingleSprite.class, registry.resolveByType("XP Boost"));
    assertEquals("data/assets/bp_xp.png", sprite.path());
  }
}
