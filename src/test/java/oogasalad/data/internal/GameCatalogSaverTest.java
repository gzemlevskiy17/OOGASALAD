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
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  }
}
