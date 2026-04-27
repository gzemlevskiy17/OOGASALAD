package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.api.LevelLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameConfigImplTest {

  @TempDir
  Path tempDir;

  private File writeConfig(String json) throws IOException {
    File file = tempDir.resolve("game.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }

  // ── Typed accessors ──────────────────────────────────────────────────────

  @Test
  void gameId_parsedFromJson() throws IOException {
    File f = writeConfig("""
        { "gameId": "badpiggies", "primaryComponentId": "body",
          "components": [], "staticComponents": [], "terrainTypes": [] }
        """);
    assertEquals("badpiggies", new GameConfigImpl(f.getAbsolutePath()).gameId());
  }

  @Test
  void primaryComponentId_parsedFromJson() throws IOException {
    File f = writeConfig("""
        { "gameId": "g", "primaryComponentId": "body",
          "components": [], "staticComponents": [], "terrainTypes": [] }
        """);
    assertEquals("body", new GameConfigImpl(f.getAbsolutePath()).primaryComponentId());
  }

  @Test
  void components_parsedWithButtonsAndMass() throws IOException {
    File f = writeConfig("""
        { "gameId": "g", "primaryComponentId": "body",
          "components": [
            { "id": "motor", "spriteUrl": "motor.png", "mass": 2.0,
              "buttons": [{ "name": "go right", "direction": {"x": 1, "y": 0} }] }
          ],
          "staticComponents": [], "terrainTypes": [] }
        """);
    var gc = new GameConfigImpl(f.getAbsolutePath());
    assertEquals(1, gc.components().size());
    var c = gc.components().get(0);
    assertEquals("motor", c.id());
    assertEquals(2.0, c.mass(), 0.001);
    assertEquals(1, c.buttons().size());
    assertEquals("go right", c.buttons().get(0).name());
    assertEquals(1.0, c.buttons().get(0).directionX(), 0.001);
  }

  @Test
  void staticComponents_parsedWithCollisionAction() throws IOException {
    File f = writeConfig("""
        { "gameId": "g", "primaryComponentId": "body",
          "components": [],
          "staticComponents": [
            { "id": "heart", "spriteUrl": "heart.png", "mass": 0,
              "onCollisionAction": { "type": "heal", "amount": 25 } }
          ],
          "terrainTypes": [] }
        """);
    var sc = new GameConfigImpl(f.getAbsolutePath()).staticComponents().get(0);
    assertEquals("heart", sc.id());
    assertNotNull(sc.onCollisionAction());
    assertEquals("heal", sc.onCollisionAction().type());
    assertEquals(25.0, sc.onCollisionAction().amount(), 0.001);
  }

  @Test
  void staticComponents_nullCollisionAction_parsedAsNull() throws IOException {
    File f = writeConfig("""
        { "gameId": "g", "primaryComponentId": "body",
          "components": [],
          "staticComponents": [
            { "id": "deco", "spriteUrl": "deco.png", "mass": 0,
              "onCollisionAction": null }
          ],
          "terrainTypes": [] }
        """);
    var sc = new GameConfigImpl(f.getAbsolutePath()).staticComponents().get(0);
    assertNull(sc.onCollisionAction());
  }

  @Test
  void terrainTypes_parsedWithFrictionAndColor() throws IOException {
    File f = writeConfig("""
        { "gameId": "g", "primaryComponentId": "body",
          "components": [], "staticComponents": [],
          "terrainTypes": [
            { "id": "ice", "spriteUrl": null, "visualColor": "#ADE8F4", "friction": 0.1 }
          ] }
        """);
    var t = new GameConfigImpl(f.getAbsolutePath()).terrainTypes().get(0);
    assertEquals("ice", t.id());
    assertEquals(0.1, t.friction(), 0.001);
    assertEquals("#ADE8F4", t.visualColor());
    assertNull(t.spriteUrl());
  }

  @Test
  void empty_returnsBlankCatalog() {
    var gc = GameConfigImpl.empty();
    assertEquals("", gc.gameId());
    assertTrue(gc.components().isEmpty());
    assertTrue(gc.staticComponents().isEmpty());
    assertTrue(gc.terrainTypes().isEmpty());
  }

  // ── Legacy dot-path API ──────────────────────────────────────────────────

  @Test
  void getSetting_topLevelKey_returnsValue() throws IOException {
    File f = writeConfig("{\"gameId\": \"badpiggies\"}");
    assertEquals("badpiggies", new GameConfigImpl(f.getAbsolutePath()).getSetting("gameId"));
  }

  @Test
  void getSetting_missingKey_returnsNull() throws IOException {
    File f = writeConfig("{\"gameId\": \"g\"}");
    assertNull(new GameConfigImpl(f.getAbsolutePath()).getSetting("nonexistent"));
  }

  @Test
  void getNumericSetting_missingKey_throwsIllegalArgument() throws IOException {
    File f = writeConfig("{\"gameId\": \"g\"}");
    assertThrows(IllegalArgumentException.class,
        () -> new GameConfigImpl(f.getAbsolutePath()).getNumericSetting("missing"));
  }

  @Test
  void constructor_nonExistentFile_throwsLevelLoadException() {
    assertThrows(LevelLoadException.class,
        () -> new GameConfigImpl("does_not_exist.json"));
  }
}
