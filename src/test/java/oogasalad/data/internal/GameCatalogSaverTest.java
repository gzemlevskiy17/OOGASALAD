package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests JSON game catalog serialization.
 */
class GameCatalogSaverTest {

  @TempDir
  Path tempDir;

  private GameCatalogSaver saver;

  @BeforeEach
  void setUp() {
    saver = new GameCatalogSaver();
  }

  private File savedFile(String name) {
    return tempDir.resolve(name).toFile();
  }

  @Test
  void save_writesGameIdAndMainCharacterId() throws IOException {
    File out = savedFile("game.json");
    saver.save(out.getAbsolutePath(), "badpiggies", "body",
        List.of(), List.of(), List.of());

    JsonNode root = new ObjectMapper().readTree(out);
    assertEquals("badpiggies", root.path("gameId").asText());
    assertEquals("body", root.path("mainCharacterId").asText());
  }

  @Test
  void save_writesComponentWithButtons() throws IOException {
    File out = savedFile("game.json");
    var btn = new ButtonInfo("go right", 1.0, 0.0);
    var comp = new ComponentInfo("motor", "motor.png", 2.0, List.of(btn));
    saver.save(out.getAbsolutePath(), "g", "motor", List.of(comp), List.of(), List.of());

    JsonNode c = new ObjectMapper().readTree(out).path("components").get(0);
    assertEquals("motor", c.path("id").asText());
    assertEquals(2.0, c.path("mass").asDouble(), 0.001);
    JsonNode b = c.path("buttons").get(0);
    assertEquals("go right", b.path("name").asText());
    assertEquals(1.0, b.path("direction").path("x").asDouble(), 0.001);
  }

  @Test
  void save_writesStaticComponentWithCollisionAction() throws IOException {
    File out = savedFile("game.json");
    var action = new CollisionAction("heal", 25.0);
    var sc = new StaticComponentInfo("heart", "heart.png", 0, action);
    saver.save(out.getAbsolutePath(), "g", "body", List.of(), List.of(sc), List.of());

    JsonNode s = new ObjectMapper().readTree(out).path("staticComponents").get(0);
    assertEquals("heart", s.path("id").asText());
    assertEquals("heal", s.path("onCollisionAction").path("type").asText());
    assertEquals(25.0, s.path("onCollisionAction").path("amount").asDouble(), 0.001);
  }

  @Test
  void save_staticComponentNullAction_writesNullNode() throws IOException {
    File out = savedFile("game.json");
    saver.save(out.getAbsolutePath(), "g", "body", List.of(),
        List.of(new StaticComponentInfo("deco", "deco.png", 0, null)), List.of());

    JsonNode action = new ObjectMapper().readTree(out)
        .path("staticComponents").get(0).path("onCollisionAction");
    assertTrue(action.isNull());
  }

  @Test
  void save_writesTerrainTypeWithFriction() throws IOException {
    File out = savedFile("game.json");
    saver.save(out.getAbsolutePath(), "g", "body", List.of(), List.of(),
        List.of(new TerrainInfo("grass", "grass.png", null, 0.6)));

    JsonNode t = new ObjectMapper().readTree(out).path("terrainTypes").get(0);
    assertEquals("grass", t.path("id").asText());
    assertEquals(0.6, t.path("friction").asDouble(), 0.001);
    assertEquals("grass.png", t.path("spriteUrl").asText());
    assertTrue(t.path("visualColor").isNull());
  }

  @Test
  void save_writesTerrainVisualColor() throws IOException {
    File out = savedFile("game.json");
    saver.save(out.getAbsolutePath(), "g", "body", List.of(), List.of(),
        List.of(new TerrainInfo("grass", null, "#00AA00", "#112233", 0.6)));

    JsonNode t = new ObjectMapper().readTree(out).path("terrainTypes").get(0);
    assertTrue(t.path("spriteUrl").isNull());
    assertEquals("#00AA00", t.path("visualColor").asText());
    assertEquals("#112233", t.path("trackColor").asText());
  }

  @Test
  void Save_CheatWithSpawnPowerupEffect_WritesCheatArray() throws IOException {
    File out = savedFile("game.json");
    CheatInfo cheat = new CheatInfo(
        "spawn_heart",
        "Spawn Heart",
        true,
        new CheatTriggerInfo(List.of("SHIFT", "P")),
        List.of(new CheatEffectInfo(
            "spawnPowerup",
            new CheatTargetInfo("world", ""),
            Map.of("componentId", "Heart", "x", "400", "y", "600"))));

    saver.save(out.getAbsolutePath(), "g", "body", List.of(), List.of(), List.of(),
        List.of(cheat));

    JsonNode cheatNode = new ObjectMapper().readTree(out).path("cheats").get(0);
    JsonNode effectNode = cheatNode.path("effects").get(0);
    assertEquals("spawn_heart", cheatNode.path("id").asText());
    assertEquals("Spawn Heart", cheatNode.path("name").asText());
    assertTrue(cheatNode.path("enabled").asBoolean());
    assertEquals("SHIFT", cheatNode.path("trigger").path("keys").get(0).asText());
    assertEquals("spawnPowerup", effectNode.path("type").asText());
    assertEquals("world", effectNode.path("target").path("kind").asText());
    assertEquals("Heart", effectNode.path("parameters").path("componentId").asText());
    assertEquals("400", effectNode.path("parameters").path("x").asText());
    assertEquals("600", effectNode.path("parameters").path("y").asText());
  }

  @Test
  void save_finishLineStaticComponent_writesFinishActionType() throws IOException {
    File out = savedFile("game.json");
    var finishAction = new CollisionAction("finish", 0);
    var goalLine = new StaticComponentInfo("GoalLine", "goal.png", 0, finishAction);
    saver.save(out.getAbsolutePath(), "g", "body",
        List.of(new ComponentInfo("body", "b.png", 1.0, List.of())),
        List.of(goalLine), List.of());

    JsonNode s = new ObjectMapper().readTree(out).path("staticComponents").get(0);
    assertEquals("GoalLine", s.path("id").asText());
    assertEquals("finish", s.path("onCollisionAction").path("type").asText());
    assertEquals(0.0, s.path("onCollisionAction").path("amount").asDouble(), 0.001);
  }

  @Test
  void roundTrip_finishLineComponent_survivesReload() throws IOException {
    File out = savedFile("game.json");
    var finishAction = new CollisionAction("finish", 0);
    var goalLine = new StaticComponentInfo("GoalLine", "goal.png", 0, finishAction);
    var comp = new ComponentInfo("body", "body.png", 1.0, List.of());
    saver.save(out.getAbsolutePath(), "testgame", "body",
        List.of(comp), List.of(goalLine), List.of());

    GameConfigImpl loaded = new GameConfigImpl(out.getAbsolutePath());
    assertEquals(1, loaded.staticComponents().size());
    StaticComponentInfo reloaded = loaded.staticComponents().get(0);
    assertEquals("GoalLine", reloaded.id());
    assertEquals("finish", reloaded.onCollisionAction().type());
  }

  @Test
  void save_damageStaticComponent_writesCorrectAction() throws IOException {
    File out = savedFile("game.json");
    var action = new CollisionAction("damage", 30.0);
    var bomb = new StaticComponentInfo("Bomb", "bomb.png", 0, action);
    saver.save(out.getAbsolutePath(), "g", "body",
        List.of(new ComponentInfo("body", "b.png", 1.0, List.of())),
        List.of(bomb), List.of());

    JsonNode s = new ObjectMapper().readTree(out).path("staticComponents").get(0);
    assertEquals("damage", s.path("onCollisionAction").path("type").asText());
    assertEquals(30.0, s.path("onCollisionAction").path("amount").asDouble(), 0.001);
  }

  @Test
  void save_scoreStaticComponent_writesCorrectAction() throws IOException {
    File out = savedFile("game.json");
    var action = new CollisionAction("score", 15.0);
    var coin = new StaticComponentInfo("Coin", "coin.png", 0, action);
    saver.save(out.getAbsolutePath(), "g", "body",
        List.of(new ComponentInfo("body", "b.png", 1.0, List.of())),
        List.of(coin), List.of());

    JsonNode s = new ObjectMapper().readTree(out).path("staticComponents").get(0);
    assertEquals("score", s.path("onCollisionAction").path("type").asText());
    assertEquals(15.0, s.path("onCollisionAction").path("amount").asDouble(), 0.001);
  }

  @Test
  void roundTrip_savedCatalogCanBeReloadedByGameConfigImpl() throws IOException {
    File out = savedFile("game.json");
    var comp = new ComponentInfo("body", "body.png", 5.0, List.of());
    var terrain = new TerrainInfo("ice", null, "#ADE8F4", 0.1);
    saver.save(out.getAbsolutePath(), "badpiggies", "body",
        List.of(comp), List.of(), List.of(terrain));

    GameConfigImpl loaded = new GameConfigImpl(out.getAbsolutePath());
    assertEquals("badpiggies", loaded.gameId());
    assertEquals("body", loaded.mainCharacterId());
    assertEquals(1, loaded.components().size());
    assertEquals("body", loaded.components().get(0).id());
    assertEquals(1, loaded.terrainTypes().size());
    assertEquals(0.1, loaded.terrainTypes().get(0).friction(), 0.001);
    assertFalse(loaded.components().isEmpty());
    assertTrue(loaded.cheats().isEmpty());
  }
}
