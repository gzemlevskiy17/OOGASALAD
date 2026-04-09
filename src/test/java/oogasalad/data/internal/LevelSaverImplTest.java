package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import oogasalad.data.api.LevelSaveException;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.GameComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelSaverImplTest {

  @TempDir
  Path tempDir;

  private LevelSaverImpl saver;

  @BeforeEach
  void setUp() {
    saver = new LevelSaverImpl();
  }

  @Test
  void saveLevel_singleTerrainBlock_writesCorrectJson() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(new TerrainBlock(0, 0, 10, 2, "assets/terrain/grass.png", true, 0.6));

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    assertTrue(output.exists());
    JsonNode root = new ObjectMapper().readTree(output);
    JsonNode terrain = root.path("level").path("terrain");
    assertEquals(1, terrain.size());
    assertEquals(0.0, terrain.get(0).path("position").path("x").asDouble(), 0.001);
    assertEquals(10.0, terrain.get(0).path("size").path("width").asDouble(), 0.001);
    assertEquals("assets/terrain/grass.png",
        terrain.get(0).path("properties").path("texture").asText());
    assertTrue(terrain.get(0).path("properties").path("tile_texture").asBoolean());
  }

  @Test
  void saveLevel_multipleBlocks_writesAllEntries() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(new TerrainBlock(0, 0, 5, 1, "a.png", true, 0.5));
    world.addObject(new TerrainBlock(5, 0, 5, 1, "b.png", false, 0.4));

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    JsonNode terrain = new ObjectMapper().readTree(output).path("level").path("terrain");
    assertEquals(2, terrain.size());
  }

  @Test
  void saveLevel_emptyWorld_writesEmptyTerrainArray() throws IOException {
    FakeWorld world = new FakeWorld();

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    JsonNode terrain = new ObjectMapper().readTree(output).path("level").path("terrain");
    assertEquals(0, terrain.size());
  }

  @Test
  void saveLevel_roundTrip_loaderRestoresSameBlockCount() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(new TerrainBlock(3, 1, 4, 2, "assets/terrain/dirt.png", false, 0.3));

    File output = tempDir.resolve("round_trip.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    FakeWorld reloaded = new FakeWorld();
    new LevelLoaderImpl().loadLevel(output.getAbsolutePath(), reloaded, new FakeGameConfig());

    assertEquals(1, reloaded.objectCount());
  }

  @Test
  void saveLevel_invalidPath_throwsLevelSaveException() {
    FakeWorld world = new FakeWorld();

    assertThrows(LevelSaveException.class,
        () -> saver.saveLevel("/invalid:/path/level.json", world));
  }

  // --- Test doubles ---

  private static class FakeGameConfig implements oogasalad.data.api.GameConfig {
    @Override public String getSetting(String key) { return null; }
    @Override public double getNumericSetting(String key) { return 0; }
  }

  private static class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();
    @Override public Collection<GameComponent> getComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<GameComponent> getAllComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<GameComponent> getActiveComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent component) { components.add(component); }
    @Override public void removeComponent(GameComponent component) { components.remove(component); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
    int objectCount() { return components.size(); }
  }
}
