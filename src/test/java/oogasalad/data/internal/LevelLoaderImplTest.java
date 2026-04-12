package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import oogasalad.data.api.LevelLoadException;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelLoaderImplTest {

  @TempDir
  Path tempDir;

  private LevelLoaderImpl loader;
  private FakeWorld world;

  @BeforeEach
  void setUp() {
    loader = new LevelLoaderImpl();
    world = new FakeWorld();
  }

  private File writeLevel(String json) throws IOException {
    File file = tempDir.resolve("level.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }

  @Test
  void loadLevel_validSingleTerrainBlock_addsOneObjectToWorld() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [
              {
                "position": { "x": 0, "y": 0 },
                "size": { "width": 10, "height": 2 },
                "properties": {
                  "texture": "assets/terrain/grass.png",
                  "tile_texture": true,
                  "friction": 0.6
                }
              }
            ]
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    assertEquals(1, world.objectCount());
  }

  @Test
  void loadLevel_multipleTerrain_addsCorrectCount() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [
              {
                "position": { "x": 0, "y": 0 },
                "size": { "width": 5, "height": 1 },
                "properties": { "texture": "a.png", "tile_texture": false, "friction": 0.5 }
              },
              {
                "position": { "x": 5, "y": 0 },
                "size": { "width": 5, "height": 1 },
                "properties": { "texture": "b.png", "tile_texture": true, "friction": 0.4 }
              }
            ]
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    assertEquals(2, world.objectCount());
  }

  @Test
  void loadLevel_emptyTerrainArray_addsNoObjects() throws IOException {
    File level = writeLevel("{\"level\": {\"terrain\": []}}");

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    assertEquals(0, world.objectCount());
  }

  @Test
  void loadLevel_missingLevelRootKey_throwsLevelLoadException() throws IOException {
    File level = writeLevel("{\"not_level\": {}}");

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig()));
  }

  @Test
  void loadLevel_nonExistentFile_throwsLevelLoadException() {
    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel("ghost_file.json", world, new FakeGameConfig()));
  }

  @Test
  void loadLevel_invalidJson_throwsLevelLoadException() throws IOException {
    File level = writeLevel("not valid json {{{{");

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig()));
  }

  @Test
  void loadLevel_terrainEntryWithZeroWidth_throwsLevelLoadException() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [
              {
                "position": { "x": 0, "y": 0 },
                "size": { "width": 0, "height": 2 },
                "properties": { "texture": "a.png", "tile_texture": false, "friction": 0.5 }
              }
            ]
          }
        }
        """);

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig()));
  }

  @Test
  void loadLevel_missingClosingBrace_throwsLevelLoadException() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0}, "size": {"width": 10, "height": 2} }
            ]
          }
        """);

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig()));
  }

  // --- Test doubles ---

  private static class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();
    @Override public List<GameComponent> getComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getAllComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getActiveComponents() { return List.copyOf(components); }
    @Override public List<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent component) { components.add(component); }
    @Override public void removeComponent(GameComponent component) { components.remove(component); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
    int objectCount() { return components.size(); }
  }

  private static class FakeGameConfig implements oogasalad.data.api.GameConfig {
    @Override public String getSetting(String key) { return null; }
    @Override public double getNumericSetting(String key) { return 0; }
  }
}
