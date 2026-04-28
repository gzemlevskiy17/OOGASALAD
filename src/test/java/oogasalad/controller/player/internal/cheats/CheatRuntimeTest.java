package oogasalad.controller.player.internal.cheats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.input.KeyCode;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.player.internal.ScoreServiceImpl;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests runtime binding and execution of author-configured cheat keys.
 *
 * @author Emilio Garcia
 */
class CheatRuntimeTest {

  private static final double ASSERTION_DELTA = 1e-6;

  @Test
  void CheatAction_MultipleEffects_ExecutesEffectsInOrder() {
    List<String> executionOrder = new ArrayList<>();
    CheatAction action = new CheatAction(
        List.of(
            context -> executionOrder.add("repair"),
            context -> executionOrder.add("spawn")),
        context(new WorldImpl(), TestGameConfig.badPiggies(), new ArrayList<>(),
            new ArrayList<>()));

    action.execute();

    assertEquals(List.of("repair", "spawn"), executionOrder,
        "Cheat effects should execute in the same order authors declare them.");
  }

  @Test
  void CheatEffectRegistry_UnknownEffectType_ThrowsCheatConfigurationException() {
    CheatEffectInfo unknownEffect = effect("unknownPrimitive", worldTarget(), Map.of());

    assertThrows(CheatConfigurationException.class,
        () -> CheatEffectRegistry.defaults().create(unknownEffect),
        "An unsupported primitive should fail during binding instead of silently doing nothing.");
  }

  @Test
  void CheatEffectRegistry_RepairEffect_RepairsTargetDamageable() {
    WorldImpl world = new WorldImpl();
    PigComponent pig = pig("BigPig", 100, 100);
    pig.damage(60);
    world.addObject(pig);
    CheatEffect repair = CheatEffectRegistry.defaults().create(
        effect("repair", mainCharacterTarget(), Map.of("amount", "25")));

    repair.apply(context(world, TestGameConfig.badPiggies(), new ArrayList<>(), new ArrayList<>()));

    assertEquals(65.0, pig.getCurrentHp(), ASSERTION_DELTA,
        "Repair should add HP to the resolved main character without exceeding max HP.");
  }

  @Test
  void CheatEffectRegistry_DamageEffect_DamagesTargetDamageable() {
    WorldImpl world = new WorldImpl();
    PigComponent pig = pig("BigPig", 100, 100);
    world.addObject(pig);
    CheatEffect damage = CheatEffectRegistry.defaults().create(
        effect("damage", mainCharacterTarget(), Map.of("amount", "30")));

    damage.apply(context(world, TestGameConfig.badPiggies(), new ArrayList<>(), new ArrayList<>()));

    assertEquals(70.0, pig.getCurrentHp(), ASSERTION_DELTA,
        "Damage should subtract HP from the resolved main character.");
  }

  @Test
  void CheatEffectRegistry_TeleportEffect_MovesTargetComponent() {
    WorldImpl world = new WorldImpl();
    StructuralComponent frame = frame("Frame", 50, 75);
    world.addObject(frame);
    CheatEffect teleport = CheatEffectRegistry.defaults().create(
        effect("teleport", componentTypeTarget("Frame"), Map.of("x", "400", "y", "275")));

    teleport.apply(context(world, TestGameConfig.badPiggies(), new ArrayList<>(), new ArrayList<>()));

    assertEquals(400.0, frame.x(), ASSERTION_DELTA,
        "Teleport should update the target component x position.");
    assertEquals(275.0, frame.y(), ASSERTION_DELTA,
        "Teleport should update the target component y position.");
  }

  @Test
  void CheatEffectRegistry_FinishRunEffect_EmitsGameWonOutcome() {
    List<Outcome> outcomes = new ArrayList<>();
    CheatEffect finishRun = CheatEffectRegistry.defaults().create(
        effect("finishRun", worldTarget(), Map.of()));

    finishRun.apply(context(new WorldImpl(), TestGameConfig.badPiggies(), outcomes,
        new ArrayList<>()));

    assertEquals(List.of(Outcome.GAME_WON), outcomes,
        "finishRun should emit the same terminal win outcome used by normal gameplay.");
  }

  @Test
  void CheatEffectRegistry_LoseRunEffect_EmitsGameLostOutcome() {
    List<Outcome> outcomes = new ArrayList<>();
    CheatEffect loseRun = CheatEffectRegistry.defaults().create(
        effect("loseRun", worldTarget(), Map.of()));

    loseRun.apply(context(new WorldImpl(), TestGameConfig.badPiggies(), outcomes,
        new ArrayList<>()));

    assertEquals(List.of(Outcome.GAME_LOST), outcomes,
        "loseRun should emit the same terminal loss outcome used by normal gameplay.");
  }

  @Test
  void CheatEffectRegistry_SetGravityEffect_ForwardsGravityValue() {
    List<Double> gravityValues = new ArrayList<>();
    CheatEffect setGravity = CheatEffectRegistry.defaults().create(
        effect("setGravity", worldTarget(), Map.of("value", "1.25")));

    setGravity.apply(context(new WorldImpl(), TestGameConfig.badPiggies(), new ArrayList<>(),
        gravityValues));

    assertEquals(List.of(1.25), gravityValues,
        "setGravity should forward the author-provided gravity value to the gameplay hook.");
  }

  @Test
  void CheatEffectRegistry_SpawnPowerupEffect_AddsPickupComponentAtConfiguredLocation() {
    WorldImpl world = new WorldImpl();
    GameConfig config = configWithStaticPowerup(new StaticComponentInfo(
        "Heart", "", 0, new CollisionAction("heal", 25)));
    CheatEffect spawnPowerup = CheatEffectRegistry.defaults().create(
        effect("spawnPowerup", worldTarget(),
            Map.of("componentId", "Heart", "x", "420", "y", "315")));

    spawnPowerup.apply(context(world, config, new ArrayList<>(), new ArrayList<>()));

    PickupComponent pickup = world.getObjects().stream()
        .filter(PickupComponent.class::isInstance)
        .map(PickupComponent.class::cast)
        .findFirst()
        .orElseThrow();
    assertEquals("Heart", pickup.getComponentType(),
        "Spawned powerup should preserve the configured static component id for sprites.");
    assertEquals(420.0, pickup.x(), ASSERTION_DELTA,
        "Spawned powerup should use the configured x position.");
    assertEquals(315.0, pickup.y(), ASSERTION_DELTA,
        "Spawned powerup should use the configured y position.");
  }

  @Test
  void CheatTargetResolver_MainCharacterTarget_FindsConfiguredTypedComponent() {
    WorldImpl world = new WorldImpl();
    PigComponent pig = pig("BigPig", 100, 100);
    world.addObject(pig);

    List<?> targets = CheatTargetResolver.defaults().resolve(mainCharacterTarget(),
        context(world, TestGameConfig.badPiggies(), new ArrayList<>(), new ArrayList<>()));

    assertEquals(1, targets.size(), "Main character targeting should find exactly the BigPig.");
    assertSame(pig, targets.getFirst(),
        "Main character targeting should prefer the configured component type.");
  }

  @Test
  void CheatTargetResolver_AllDamageableTarget_ReturnsDamageableObjects() {
    WorldImpl world = new WorldImpl();
    PigComponent pig = pig("BigPig", 100, 100);
    StructuralComponent frame = frame("Frame", 50, 75);
    world.addObject(pig);
    world.addObject(frame);

    List<?> targets = CheatTargetResolver.defaults().resolve(
        new CheatTargetInfo("allDamageable", ""),
        context(world, TestGameConfig.badPiggies(), new ArrayList<>(), new ArrayList<>()));

    assertTrue(targets.containsAll(List.of(pig, frame)),
        "allDamageable should include every active component that supports HP changes.");
  }

  @Test
  void CheatInputHandler_KeyChordPressed_FiresActionOnceUntilReleased() {
    int[] executionCount = {0};
    CheatInputHandler handler = new CheatInputHandler(List.of(
        new CheatKeyBinding(Set.of(KeyCode.SHIFT, KeyCode.H), () -> executionCount[0]++)));

    handler.handleKeyPressed(KeyCode.SHIFT);
    handler.handleKeyPressed(KeyCode.H);
    handler.handleKeyPressed(KeyCode.H);
    handler.handleKeyReleased(KeyCode.H);
    handler.handleKeyPressed(KeyCode.H);

    assertEquals(2, executionCount[0],
        "A held chord should execute once, then become eligible again after release.");
  }

  @Test
  void CheatKeyBinder_DisabledCheat_IgnoresCheat() {
    WorldImpl world = new WorldImpl();
    List<Outcome> outcomes = new ArrayList<>();
    CheatInfo disabledCheat = cheat(false, "finishRun", worldTarget(), Map.of(), "SHIFT", "W");
    CheatInputHandler handler = CheatKeyBinder.defaults().bind(
        configWithCheats(List.of(disabledCheat)),
        context(world, configWithCheats(List.of(disabledCheat)), outcomes, new ArrayList<>()));

    handler.handleKeyPressed(KeyCode.SHIFT);
    handler.handleKeyPressed(KeyCode.W);

    assertTrue(outcomes.isEmpty(),
        "Disabled cheats should not create active runtime key bindings.");
  }

  @Test
  void CheatKeyBinder_InvalidKeyName_ThrowsCheatConfigurationException() {
    CheatInfo cheat = cheat(true, "finishRun", worldTarget(), Map.of(), "NOT_A_KEY");

    assertThrows(CheatConfigurationException.class,
        () -> CheatKeyBinder.defaults().bind(configWithCheats(List.of(cheat)),
            context(new WorldImpl(), configWithCheats(List.of(cheat)), new ArrayList<>(),
                new ArrayList<>())),
        "Invalid JavaFX key names should fail while binding the author configuration.");
  }

  @Test
  void CheatKeyBinder_EnabledSpawnPowerupCheat_BuildsKeyBinding() {
    WorldImpl world = new WorldImpl();
    CheatInfo spawnCheat = cheat(true, "spawnPowerup", worldTarget(),
        Map.of("componentId", "Heart", "x", "275", "y", "350"), "SHIFT", "P");
    GameConfig config = configWithCheatsAndStaticPowerup(List.of(spawnCheat),
        new StaticComponentInfo("Heart", "", 0, new CollisionAction("heal", 25)));
    CheatInputHandler handler = CheatKeyBinder.defaults().bind(config,
        context(world, config, new ArrayList<>(), new ArrayList<>()));

    handler.handleKeyPressed(KeyCode.SHIFT);
    handler.handleKeyPressed(KeyCode.P);

    long spawnedPowerups = world.getObjects().stream()
        .filter(PickupComponent.class::isInstance)
        .count();
    assertEquals(1, spawnedPowerups,
        "An enabled spawnPowerup cheat should create a runtime key binding that spawns one pickup.");
  }

  private CheatRuntimeContext context(World world, GameConfig config, List<Outcome> outcomes,
      List<Double> gravityValues) {
    return new CheatRuntimeContext(world, config, config.mainCharacterId(), outcomes::add,
        gravityValues::add, new ScoreServiceImpl());
  }

  private CheatEffectInfo effect(String type, CheatTargetInfo target,
      Map<String, String> parameters) {
    return new CheatEffectInfo(type, target, parameters);
  }

  private CheatTargetInfo worldTarget() {
    return new CheatTargetInfo("world", "");
  }

  private CheatTargetInfo mainCharacterTarget() {
    return new CheatTargetInfo("mainCharacter", "");
  }

  private CheatTargetInfo componentTypeTarget(String id) {
    return new CheatTargetInfo("componentType", id);
  }

  private CheatInfo cheat(boolean enabled, String effectType, CheatTargetInfo target,
      Map<String, String> parameters, String... keys) {
    return new CheatInfo(effectType + "_cheat", effectType, enabled,
        new CheatTriggerInfo(List.of(keys)),
        List.of(effect(effectType, target, parameters)));
  }

  private GameConfig configWithStaticPowerup(StaticComponentInfo staticComponentInfo) {
    return configWithCheatsAndStaticPowerup(List.of(), staticComponentInfo);
  }

  private GameConfig configWithCheats(List<CheatInfo> cheats) {
    return new TestGameConfig("badpiggies", "BigPig",
        List.of(TestGameConfig.component("BigPig")),
        List.of(),
        List.of(),
        cheats);
  }

  private GameConfig configWithCheatsAndStaticPowerup(List<CheatInfo> cheats,
      StaticComponentInfo staticComponentInfo) {
    return new TestGameConfig("badpiggies", "BigPig",
        List.of(TestGameConfig.component("BigPig")),
        List.of(staticComponentInfo),
        List.of(),
        cheats);
  }

  private PigComponent pig(String componentType, double x, double y) {
    return new PigComponent(x, y, 0,
        ConvexPolygon.axisAlignedBox(x - 20, y - 20, 40, 40),
        "",
        0.5,
        2.0,
        1.0,
        0.5,
        true,
        100.0,
        componentType);
  }

  private StructuralComponent frame(String componentType, double x, double y) {
    return new StructuralComponent(x, y, 0,
        ConvexPolygon.rotatedBox(x, y, 40, 40, 0),
        "",
        0.5,
        1.0,
        1.0,
        0.5,
        true,
        100.0,
        componentType,
        40,
        40);
  }
}
