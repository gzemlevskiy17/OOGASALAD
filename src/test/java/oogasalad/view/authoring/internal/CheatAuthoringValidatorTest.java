package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.StaticComponentInfo;
import org.junit.jupiter.api.Test;

/**
 * Tests authoring-time validation of cheat definitions before catalog persistence.
 *
 * @author Emilio Garcia
 */
class CheatAuthoringValidatorTest {

  private final CheatAuthoringValidator validator = new CheatAuthoringValidator();

  @Test
  void ValidateCheat_ValidSpawnPowerupWithCoordinates_DoesNotThrow() {
    CheatInfo cheat = cheat(List.of("SHIFT", "P"),
        effect("spawnPowerup", worldTarget(),
            Map.of("componentId", "Heart", "x", "400", "y", "600")));

    assertDoesNotThrow(() -> validator.validateCheat(cheat, staticComponents()),
        "Valid authoring input should be accepted before saving.");
  }

  @Test
  void ValidateCheat_InvalidKeyName_ThrowsCheatAuthoringException() {
    CheatInfo cheat = cheat(List.of("NOT_A_KEY"),
        effect("finishRun", worldTarget(), Map.of()));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateCheat(cheat, staticComponents()),
        "Invalid JavaFX key names should be rejected in authoring.");
  }

  @Test
  void ValidateEffect_UnknownEffectType_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("unknownPrimitive", worldTarget(), Map.of());

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "Unsupported runtime primitives should be rejected before staging an effect.");
  }

  @Test
  void ValidateEffect_UnknownTargetKind_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("repair", new CheatTargetInfo("nearestEnemy", ""),
        Map.of("amount", "25"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "Unsupported target selectors should be rejected before staging an effect.");
  }

  @Test
  void ValidateEffect_TeleportMissingCoordinate_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("teleport", componentTypeTarget("Frame"),
        Map.of("x", "100"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "Teleport effects should require both coordinates.");
  }

  @Test
  void ValidateEffect_AddScoreWithIntegerAmount_DoesNotThrow() {
    CheatEffectInfo effect = effect("addScore", worldTarget(), Map.of("amount", "150.0"));

    assertDoesNotThrow(() -> validator.validateEffect(effect, staticComponents()),
        "addScore should accept integer-compatible score amounts.");
  }

  @Test
  void ValidateEffect_AddScoreWithDecimalAmount_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("addScore", worldTarget(), Map.of("amount", "12.5"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "addScore should reject non-integer score amounts.");
  }

  @Test
  void ValidateEffect_DestroyWithSupportedTarget_DoesNotThrow() {
    CheatEffectInfo effect = effect("destroy", componentTypeTarget("Frame"), Map.of());

    assertDoesNotThrow(() -> validator.validateEffect(effect, staticComponents()),
        "destroy should not require parameters beyond a supported target selector.");
  }

  @Test
  void ValidateEffect_LaunchWithVelocity_DoesNotThrow() {
    CheatEffectInfo effect = effect("launch", componentTypeTarget("Frame"),
        Map.of("xVelocity", "250", "yVelocity", "-125"));

    assertDoesNotThrow(() -> validator.validateEffect(effect, staticComponents()),
        "launch should accept numeric velocity components.");
  }

  @Test
  void ValidateEffect_LaunchMissingVelocity_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("launch", componentTypeTarget("Frame"),
        Map.of("xVelocity", "250"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "launch should require both velocity components.");
  }

  @Test
  void ValidateEffect_SpawnPowerupUnknownStaticComponent_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("spawnPowerup", worldTarget(),
        Map.of("componentId", "Missing", "x", "400", "y", "600"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "spawnPowerup should only reference static component ids in the active catalog.");
  }

  @Test
  void ValidateEffect_SpawnPowerupWithoutCoordinatesOnWorld_ThrowsCheatAuthoringException() {
    CheatEffectInfo effect = effect("spawnPowerup", worldTarget(), Map.of("componentId", "Heart"));

    assertThrows(CheatAuthoringException.class,
        () -> validator.validateEffect(effect, staticComponents()),
        "spawnPowerup without x/y should require an object target.");
  }

  private CheatInfo cheat(List<String> keys, CheatEffectInfo effect) {
    return new CheatInfo("cheat", "Cheat", true, new CheatTriggerInfo(keys), List.of(effect));
  }

  private CheatEffectInfo effect(String type, CheatTargetInfo target, Map<String, String> params) {
    return new CheatEffectInfo(type, target, params);
  }

  private CheatTargetInfo worldTarget() {
    return new CheatTargetInfo("world", "");
  }

  private CheatTargetInfo componentTypeTarget(String componentId) {
    return new CheatTargetInfo("componentType", componentId);
  }

  private List<StaticComponentInfo> staticComponents() {
    return List.of(new StaticComponentInfo("Heart", "heart.png", 0,
        new CollisionAction("heal", 25)));
  }
}
