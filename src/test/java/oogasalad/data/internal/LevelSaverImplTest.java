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
import java.util.List;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelSaveException;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
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
    world.addObject(
        new TerrainBlock(0, 0, 10, 2, "assets/terrain/grass.png", true, 0.6, 0.0));
    LevelAttributes levelAttributes = new LevelAttributes();

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, levelAttributes);

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
    world.addObject(new TerrainBlock(0, 0, 5, 1, "a.png", true, 0.5, 0.0));
    world.addObject(new TerrainBlock(5, 0, 5, 1, "b.png", false, 0.4, 0.0));
    LevelAttributes levelAttributes = new LevelAttributes();

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, levelAttributes);

    JsonNode terrain = new ObjectMapper().readTree(output).path("level").path("terrain");
    assertEquals(2, terrain.size());
  }

  @Test
  void saveLevel_emptyWorld_writesEmptyTerrainArray() throws IOException {
    FakeWorld world = new FakeWorld();
    LevelAttributes levelAttributes = new LevelAttributes();

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, levelAttributes);

    JsonNode terrain = new ObjectMapper().readTree(output).path("level").path("terrain");
    assertEquals(0, terrain.size());
  }

  @Test
  void saveLevel_roundTrip_loaderRestoresSameBlockCount() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(
        new TerrainBlock(3, 1, 4, 2, "assets/terrain/dirt.png", false, 0.3, 0.0));
    LevelAttributes savedAttributes = new LevelAttributes();
    savedAttributes.setMaxSelectableVehicleItems(5);
    savedAttributes.setAllowedVehicleComponents(List.of("pig", "wheel"));

    File output = tempDir.resolve("round_trip.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, savedAttributes);

    FakeWorld reloaded = new FakeWorld();
    LevelAttributes reloadedAttributes = new LevelAttributes();
    new LevelLoaderImpl().loadLevel(output.getAbsolutePath(), reloaded, reloadedAttributes,
        new FakeGameConfig());

    assertEquals(1, reloaded.objectCount());
    assertEquals(List.of("pig", "wheel"), reloadedAttributes.getAllowedVehicleComponents());
  }

  @Test
  void saveLevel_invalidPath_throwsLevelSaveException() {
    FakeWorld world = new FakeWorld();
    LevelAttributes levelAttributes = new LevelAttributes();

    assertThrows(LevelSaveException.class,
        () -> saver.saveLevel("/invalid:/path/level.json", world, levelAttributes));
  }

  @Test
  void saveLevel_vehiclePalette_writesVehiclePaletteBlock() throws IOException {
    FakeWorld world = new FakeWorld();
    LevelAttributes levelAttributes = new LevelAttributes();
    levelAttributes.setMaxSelectableVehicleItems(5);
    levelAttributes.setAllowedVehicleComponents(List.of("pig", "wheel", "fan"));

    File output = tempDir.resolve("saved_level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, levelAttributes);

    JsonNode vehiclePalette = new ObjectMapper().readTree(output).path("level").path("vehicle_palette");
    assertEquals(5, vehiclePalette.path("max_selectable").asInt());
    assertEquals(3, vehiclePalette.path("allowed_components").size());
  }

  // --- Spline serialization ---

  @Test
  void saveLevel_terrainSpline_writesSplineEntry() throws IOException {
    FakeWorld world = new FakeWorld();
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(20, 15), new Vector(40, 10)), 100.0, 6);
    world.addObject(new TerrainSpline(spline, "assets/dirt.png", true, 0.7, "#ffaa00"));

    File output = tempDir.resolve("splined.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    JsonNode root = new ObjectMapper().readTree(output);
    JsonNode splines = root.path("level").path("splines");

    assertEquals(1, splines.size());
    JsonNode entry = splines.get(0);
    assertEquals(3, entry.path("knots").size());
    assertEquals(2, entry.path("controls").size());
    assertEquals(100.0, entry.path("baselineY").asDouble(), 0.001);
    assertEquals(6, entry.path("resolution").asInt());
    assertEquals("assets/dirt.png", entry.path("properties").path("texture").asText());
    assertTrue(entry.path("properties").path("tile_texture").asBoolean());
    assertEquals("#ffaa00", entry.path("properties").path("color").asText());
  }

  @Test
  void saveLevel_terrainSpline_withScaleFactor_convertsCoordinatesToWorldUnits() throws IOException {
    LevelSaverImpl scalingSaver = new LevelSaverImpl(20.0); // simulate authoring cellSize=20
    FakeWorld world = new FakeWorld();
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(1, 2), new Vector(5, 3)), 10.0, 4);
    world.addObject(new TerrainSpline(spline, "", false, 0.5, "#fff"));

    File output = tempDir.resolve("scaled.json").toFile();
    scalingSaver.saveLevel(output.getAbsolutePath(), world);

    JsonNode entry = new ObjectMapper().readTree(output).path("level").path("splines").get(0);
    JsonNode firstKnot = entry.path("knots").get(0);

    assertEquals(20.0, firstKnot.path("x").asDouble(), 0.001);
    assertEquals(40.0, firstKnot.path("y").asDouble(), 0.001);
    assertEquals(200.0, entry.path("baselineY").asDouble(), 0.001);
  }

  @Test
  void saveLevel_noSplines_writesEmptySplineArray() throws IOException {
    FakeWorld world = new FakeWorld();
    File output = tempDir.resolve("empty_splines.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    JsonNode splines = new ObjectMapper().readTree(output).path("level").path("splines");
    assertEquals(0, splines.size());
  }

  @Test
  void saveLevel_mixedBlockAndSpline_writesBothInCorrectSections() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(new TerrainBlock(0, 0, 10, 2, "a.png", true, 0.5, 0.0));
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(20, 10)), 100.0, 4);
    world.addObject(new TerrainSpline(spline, "", false, 0.5, "#fff"));

    File output = tempDir.resolve("mixed.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world);

    JsonNode level = new ObjectMapper().readTree(output).path("level");
    assertEquals(1, level.path("terrain").size());
    assertEquals(1, level.path("splines").size());
  }

  // --- Test doubles ---

  private static class FakeGameConfig implements oogasalad.data.api.GameConfig {
    @Override public String getSetting(String key) { return null; }
    @Override public double getNumericSetting(String key) { return 0; }
  }

  private static class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();

    @Override
    public List<GameComponent> getComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getAllComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getActiveComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<Joint> getJoints() {
      return List.of();
    }
    @Override public void addComponent(GameComponent component) { components.add(component); }
    @Override public void removeComponent(GameComponent component) { components.remove(component); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
    int objectCount() { return components.size(); }
  }
}
