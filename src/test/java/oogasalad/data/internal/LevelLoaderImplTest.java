package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoadException;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelLoaderImplTest {

  @TempDir
  Path tempDir;

  private LevelLoaderImpl loader;
  private FakeWorld world;
  private LevelAttributes levelAttributes;

  @BeforeEach
  void setUp() {
    loader = new LevelLoaderImpl();
    world = new FakeWorld();
    levelAttributes = new LevelAttributes();
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

    loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig());

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

    loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig());

    assertEquals(2, world.objectCount());
  }

  @Test
  void loadLevel_emptyTerrainArray_addsNoObjects() throws IOException {
    File level = writeLevel("{\"level\": {\"terrain\": []}}");

    loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig());

    assertEquals(0, world.objectCount());
  }

  @Test
  void loadLevel_missingLevelRootKey_throwsLevelLoadException() throws IOException {
    File level = writeLevel("{\"not_level\": {}}");

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig()));
  }

  @Test
  void loadLevel_nonExistentFile_throwsLevelLoadException() {
    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel("ghost_file.json", world, levelAttributes, new FakeGameConfig()));
  }

  @Test
  void loadLevel_invalidJson_throwsLevelLoadException() throws IOException {
    File level = writeLevel("not valid json {{{{");

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig()));
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
        () -> loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig()));
  }

  // --- Spline deserialization ---

  @Test
  void loadLevel_missingSplinesArray_doesNotFail() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": []
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());
    // Must not have added any TerrainSpline instances.
    assertTrue(world.components.stream().noneMatch(c -> c instanceof TerrainSpline));
  }

  @Test
  void loadLevel_splineWithTwoKnots_createsTerrainSpline() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "splines": [
              {
                "knots": [
                  { "x": 0,   "y": 10 },
                  { "x": 100, "y": 20 }
                ],
                "controls": [
                  { "x": 50, "y": 15 }
                ],
                "baselineY": 200,
                "resolution": 8,
                "properties": {
                  "texture": "assets/dirt.png",
                  "tile_texture": true,
                  "friction": 0.75,
                  "color": "#aabbcc"
                }
              }
            ]
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    TerrainSpline spline = (TerrainSpline) world.components.stream()
        .filter(c -> c instanceof TerrainSpline).findFirst().orElseThrow();
    assertEquals(2, spline.getSpline().knots().size());
    assertEquals(200.0, spline.getSpline().baselineY(), 0.001);
    assertEquals(8, spline.getSpline().segmentsPerKnotPair());
    assertEquals("assets/dirt.png", spline.getSprite());
    assertTrue(spline.isTiled());
    assertEquals("#aabbcc", spline.getColor());
  }

  @Test
  void loadLevel_splineWithScaleFactor_invertsCoordinatesToGridUnits() throws IOException {
    LevelLoaderImpl scaling = new LevelLoaderImpl(20.0);
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "splines": [
              {
                "knots": [
                  { "x": 20, "y": 40 },
                  { "x": 80, "y": 40 }
                ],
                "controls": [],
                "baselineY": 200,
                "resolution": 4,
                "properties": {}
              }
            ]
          }
        }
        """);

    scaling.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    TerrainSpline spline = (TerrainSpline) world.components.stream()
        .filter(c -> c instanceof TerrainSpline).findFirst().orElseThrow();
    // Scale 20 should take world-pixel knots (20, 40) back down to grid-cell (1, 2).
    assertEquals(1.0, spline.getSpline().knots().get(0).x(), 0.001);
    assertEquals(2.0, spline.getSpline().knots().get(0).y(), 0.001);
    assertEquals(10.0, spline.getSpline().baselineY(), 0.001);
  }

  @Test
  void loadLevel_splineWithFewerControlsThanExpected_fallsBackToDefaultMidpoints() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "splines": [
              {
                "knots": [
                  { "x": 0,   "y": 10 },
                  { "x": 100, "y": 10 },
                  { "x": 200, "y": 10 }
                ],
                "controls": [],
                "baselineY": 200,
                "resolution": 4,
                "properties": {}
              }
            ]
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    TerrainSpline spline = (TerrainSpline) world.components.stream()
        .filter(c -> c instanceof TerrainSpline).findFirst().orElseThrow();
    // Default midpoint controls: two controls at (50, 10) and (150, 10).
    assertEquals(2, spline.getSpline().controls().size());
    assertEquals(50.0, spline.getSpline().controls().get(0).x(), 0.001);
    assertEquals(150.0, spline.getSpline().controls().get(1).x(), 0.001);
  }

  @Test
  void loadLevel_splineWithLessThanTwoKnots_throwsLevelLoadException() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "splines": [
              {
                "knots": [ { "x": 0, "y": 10 } ],
                "controls": [],
                "baselineY": 200,
                "resolution": 4,
                "properties": {}
              }
            ]
          }
        }
        """);

    assertThrows(LevelLoadException.class,
        () -> loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig()));
  }

  @Test
  void loadLevel_multipleSplines_createsAllOfThem() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "splines": [
              { "knots": [{"x":0,"y":10},{"x":50,"y":10}], "controls": [], "baselineY": 100, "resolution": 4, "properties": {} },
              { "knots": [{"x":60,"y":10},{"x":120,"y":10}], "controls": [], "baselineY": 100, "resolution": 4, "properties": {} }
            ]
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, new FakeGameConfig());

    long count = world.components.stream().filter(c -> c instanceof TerrainSpline).count();
    assertEquals(2, count);
    assertFalse(count == 0);
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
        () -> loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig()));
  }

  @Test
  void loadLevel_vehiclePalette_populatesLevelAttributes() throws IOException {
    File level = writeLevel("""
        {
          "level": {
            "terrain": [],
            "vehicle_palette": {
              "max_selectable": 5,
              "allowed_components": ["pig", "wheel", "fan"]
            }
          }
        }
        """);

    loader.loadLevel(level.getAbsolutePath(), world, levelAttributes, new FakeGameConfig());

    assertEquals(5, levelAttributes.getMaxSelectableVehicleItems());
    assertEquals(List.of("pig", "wheel", "fan"), levelAttributes.getAllowedVehicleComponents());
  }

  // --- Test doubles ---

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

    @Override
    public double getDragCoefficient() {
      return 0.0;
    }
  }

  private static class FakeGameConfig implements oogasalad.data.api.GameConfig {
    @Override public String getSetting(String key) { return null; }
    @Override public double getNumericSetting(String key) { return 0; }
  }
}
