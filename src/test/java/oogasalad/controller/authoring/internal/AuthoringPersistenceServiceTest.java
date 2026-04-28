package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelSaverImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the save and load paths in the authoring environment. Uses real LevelLoaderImpl,
 * LevelSaverImpl, and GameLoaderImpl with temp files. The AuthoringCanvas is mocked to avoid JavaFX
 * dependencies.
 *
 * @author Paulina Vvedenskaya
 */
class AuthoringPersistenceServiceTest {

  private static final double CELL_SIZE = 60.0;

  @TempDir
  Path tempDir;

  private World world;
  private LevelAttributes levelAttributes;
  private CanvasActionHistory history;
  private AuthoringCanvas canvas;
  private AuthoringPersistenceService service;

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    levelAttributes = new LevelAttributes();
    history = new CanvasActionHistory();
    canvas = mock(AuthoringCanvas.class);
    when(canvas.getCellSize()).thenReturn(CELL_SIZE);
    when(canvas.isWithinBounds(org.mockito.ArgumentMatchers.anyInt(),
        org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);

    SpawnPointService spawnPointService = new SpawnPointService(world);

    service = new AuthoringPersistenceService(
        new LevelLoaderImpl(),
        new LevelSaverImpl(CELL_SIZE),
        new GameLoaderImpl(),
        world,
        levelAttributes,
        spawnPointService,
        history,
        canvas);
  }

  // ── Load from existing file ──────────────────────────────────────────────

  @Test
  void load_populatesWorldWithTerrainSpline() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    List<TerrainSpline> splines = filterByType(world.getObjects(), TerrainSpline.class);
    assertFalse(splines.isEmpty(), "World should contain at least one TerrainSpline after load");
  }

  @Test
  void load_populatesWorldWithSpawnPoint() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    List<SpawnPointComponent> spawns = filterByType(world.getObjects(), SpawnPointComponent.class);
    assertFalse(spawns.isEmpty(), "World should contain a SpawnPointComponent after load");
  }

  @Test
  void load_populatesWorldWithStaticObjects() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    List<AuthoringStaticComponent> statics =
        filterByType(world.getObjects(), AuthoringStaticComponent.class);
    assertTrue(statics.size() >= 2,
        "Expected at least 2 AuthoringStaticComponents (GoalLine + Coin), got " + statics.size());
  }

  @Test
  void load_populatesLevelAttributes() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    assertEquals("Test Level", levelAttributes.getLevelName());
    assertEquals("badpiggies", levelAttributes.getGameType());
    assertEquals(1920.0, levelAttributes.getScreenWidth(), 0.01);
    assertEquals(1080.0, levelAttributes.getScreenHeight(), 0.01);
  }

  @Test
  void load_refreshesCanvasAfterLoad() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    verify(canvas, atLeastOnce()).refreshGrid();
  }

  @Test
  void load_scalesSpawnPointToAuthoringSpace() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    List<SpawnPointComponent> spawns = filterByType(world.getObjects(), SpawnPointComponent.class);
    SpawnPointComponent fileSpawn = null;
    for (SpawnPointComponent s : spawns) {
      if (Math.abs(s.x() - 100.0 / CELL_SIZE) < 0.1) {
        fileSpawn = s;
        break;
      }
    }
    assertNotNull(fileSpawn,
        "Should find spawn point scaled to cell coords (~" + (100.0 / CELL_SIZE) + ")");
    assertEquals(100.0 / CELL_SIZE, fileSpawn.x(), 0.01);
    assertEquals(800.0 / CELL_SIZE, fileSpawn.y(), 0.01);
  }

  @Test
  void load_scalesTerrainSplineKnotsToAuthoringSpace() throws IOException {
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    List<TerrainSpline> splines = filterByType(world.getObjects(), TerrainSpline.class);
    TerrainSpline spline = splines.get(0);
    double firstKnotX = spline.getSpline().knots().get(0).x();
    assertEquals(100.0 / CELL_SIZE, firstKnotX, 0.01,
        "Spline knot X should be scaled from pixel to cell coords");
  }

  @Test
  void load_clearsWorldBeforePopulating() throws IOException {
    world.addComponent(new SpawnPointComponent(99, 99));
    String levelPath = writeSampleLevel();

    service.load(levelPath);

    for (GameObject object : world.getObjects()) {
      if (object instanceof SpawnPointComponent spawn) {
        assertTrue(spawn.x() < 50,
            "Old spawn at (99,99) should have been cleared; found spawn at ("
                + spawn.x() + "," + spawn.y() + ")");
      }
    }
  }

  // ── Save ─────────────────────────────────────────────────────────────────

  @Test
  void save_writesFileToSpecifiedPath() {
    populateWorldForSave();
    String savePath = tempDir.resolve("saved_level.json").toString();

    service.save(savePath);

    assertTrue(Files.exists(Path.of(savePath)), "Save should create the level file");
  }

  @Test
  void save_outputContainsGameMetadata() throws IOException {
    levelAttributes.setLevelName("My Level");
    levelAttributes.setGameType("badpiggies");
    levelAttributes.setScreenWidth(1920.0);
    levelAttributes.setScreenHeight(1080.0);
    populateWorldForSave();
    String savePath = tempDir.resolve("saved_level.json").toString();

    service.save(savePath);

    String json = Files.readString(Path.of(savePath));
    assertTrue(json.contains("My Level"), "Saved JSON should contain level name");
    assertTrue(json.contains("badpiggies"), "Saved JSON should contain game type");
  }

  @Test
  void save_outputContainsSpawnPointInPixelCoords() throws IOException {
    world.addComponent(new SpawnPointComponent(3.0, 5.0));
    String savePath = tempDir.resolve("saved_level.json").toString();

    service.save(savePath);

    String json = Files.readString(Path.of(savePath));
    assertTrue(json.contains(String.valueOf(3.0 * CELL_SIZE))
            || json.contains(String.valueOf((int) (3.0 * CELL_SIZE))),
        "Spawn X should be scaled to pixel coords (" + (3.0 * CELL_SIZE) + ") in saved JSON");
  }

  @Test
  void save_outputContainsSplineKnotsInPixelCoords() throws IOException {
    List<Vector> knots = List.of(
        new Vector(2.0, 10.0),
        new Vector(5.0, 8.0),
        new Vector(10.0, 12.0));
    ParabolaSpline spline = new ParabolaSpline(knots, 15.0);
    TerrainSpline terrain = new TerrainSpline("GrassHills", spline, "", true, 0.6, "#4A7C59");
    world.addComponent(terrain);
    String savePath = tempDir.resolve("saved_level.json").toString();

    service.save(savePath);

    String json = Files.readString(Path.of(savePath));
    double expectedFirstKnotX = 2.0 * CELL_SIZE;
    assertTrue(json.contains(String.valueOf(expectedFirstKnotX)),
        "First knot X should be " + expectedFirstKnotX + " in saved JSON, got:\n" + json);
  }

  // ── Save → Load round-trip ───────────────────────────────────────────────

  @Test
  void roundTrip_spawnPointPreservesPosition() throws IOException {
    SpawnPointComponent originalSpawn = new SpawnPointComponent(3.0, 5.0);
    world.addComponent(originalSpawn);
    addMinimalSpline();
    levelAttributes.setGameType("badpiggies");
    levelAttributes.setScreenWidth(1920.0);
    levelAttributes.setScreenHeight(1080.0);
    String savePath = tempDir.resolve("roundtrip.json").toString();

    service.save(savePath);
    service.load(savePath);

    List<SpawnPointComponent> spawns = filterByType(world.getObjects(), SpawnPointComponent.class);
    SpawnPointComponent loaded = spawns.get(0);
    assertEquals(3.0, loaded.x(), 0.1,
        "Spawn X should survive save→load round-trip");
    assertEquals(5.0, loaded.y(), 0.1,
        "Spawn Y should survive save→load round-trip");
  }

  @Test
  void roundTrip_splineKnotsPreservePositions() throws IOException {
    List<Vector> knots = List.of(
        new Vector(2.0, 10.0),
        new Vector(5.0, 8.0),
        new Vector(10.0, 12.0));
    ParabolaSpline spline = new ParabolaSpline(knots, 15.0);
    TerrainSpline terrain = new TerrainSpline("GrassHills", spline, "", true, 0.6, "#4A7C59");
    world.addComponent(terrain);
    world.addComponent(new SpawnPointComponent(1, 1));
    levelAttributes.setGameType("badpiggies");
    levelAttributes.setScreenWidth(1920.0);
    levelAttributes.setScreenHeight(1080.0);
    String savePath = tempDir.resolve("roundtrip_spline.json").toString();

    service.save(savePath);
    service.load(savePath);

    List<TerrainSpline> splines = filterByType(world.getObjects(), TerrainSpline.class);
    assertFalse(splines.isEmpty(), "Spline should survive round-trip");
    TerrainSpline loaded = splines.get(0);
    assertEquals(2.0, loaded.getSpline().knots().get(0).x(), 0.1,
        "First knot X should survive save→load round-trip");
    assertEquals(10.0, loaded.getSpline().knots().get(0).y(), 0.1,
        "First knot Y should survive save→load round-trip");
  }

  @Test
  void roundTrip_staticObjectsPreservePositions() throws IOException {
    AuthoringStaticComponent coin = new AuthoringStaticComponent(
        4.0, 6.0, 1.0, 1.0, "Coin", "#F5C542", "data/assets/bp_coin.png");
    world.addComponent(coin);
    addMinimalSpline();
    world.addComponent(new SpawnPointComponent(1, 1));
    levelAttributes.setGameType("badpiggies");
    levelAttributes.setScreenWidth(1920.0);
    levelAttributes.setScreenHeight(1080.0);
    String savePath = tempDir.resolve("roundtrip_static.json").toString();

    service.save(savePath);
    service.load(savePath);

    List<AuthoringStaticComponent> statics =
        filterByType(world.getObjects(), AuthoringStaticComponent.class);
    assertFalse(statics.isEmpty(), "Static objects should survive round-trip");
    AuthoringStaticComponent loaded = statics.get(0);
    assertEquals("Coin", loaded.getComponentType(),
        "Static component type should survive round-trip");
    assertEquals(4.0, loaded.x(), 0.5,
        "Static X should survive save→load round-trip");
    assertEquals(6.0, loaded.y(), 0.5,
        "Static Y should survive save→load round-trip");
  }

  @Test
  void roundTrip_levelAttributesPreserveValues() throws IOException {
    levelAttributes.setLevelName("Round Trip Level");
    levelAttributes.setGameType("badpiggies");
    levelAttributes.setScreenWidth(1920.0);
    levelAttributes.setScreenHeight(1080.0);
    levelAttributes.setGravity(980.0);
    levelAttributes.setDrag(0.1);
    levelAttributes.setMaxHP(5.0);
    levelAttributes.setMaxSelectable(8);
    addMinimalSpline();
    world.addComponent(new SpawnPointComponent(1, 1));
    String savePath = tempDir.resolve("roundtrip_attrs.json").toString();

    service.save(savePath);
    service.load(savePath);

    assertEquals("Round Trip Level", levelAttributes.getLevelName());
    assertEquals("badpiggies", levelAttributes.getGameType());
    assertEquals(1920.0, levelAttributes.getScreenWidth(), 0.01);
    assertEquals(1080.0, levelAttributes.getScreenHeight(), 0.01);
    assertEquals(980.0, levelAttributes.getGravity(), 0.01);
    assertEquals(0.1, levelAttributes.getDrag(), 0.01);
    assertEquals(5.0, levelAttributes.getMaxHP(), 0.01);
    assertEquals(8, levelAttributes.getMaxSelectable());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private void populateWorldForSave() {
    addMinimalSpline();
    world.addComponent(new SpawnPointComponent(5, 5));
  }

  private void addMinimalSpline() {
    List<Vector> knots = List.of(
        new Vector(2.0, 10.0),
        new Vector(8.0, 10.0));
    ParabolaSpline spline = new ParabolaSpline(knots, 15.0);
    world.addComponent(
        new TerrainSpline("GrassHills", spline, "", true, 0.6, "#4A7C59"));
  }

  private String writeSampleLevel() throws IOException {
    Path levelFile = tempDir.resolve("test_level.json");
    Files.writeString(levelFile, """
        {
          "gameMetadata": {
            "levelName": "Test Level",
            "gameType": "badpiggies",
            "screenWidth": 1920.0,
            "screenHeight": 1080.0,
            "gravity": 980.0,
            "drag": 0.1
          },
          "map": {
            "knotLocations": [
              {"x": 100.0, "y": 900.0},
              {"x": 500.0, "y": 850.0},
              {"x": 900.0, "y": 920.0},
              {"x": 1400.0, "y": 880.0}
            ],
            "objects": [
              {
                "type": "Coin",
                "position": {"x": 400.0, "y": 860.0}
              },
              {
                "type": "GoalLine",
                "position": {"x": 1300.0, "y": 870.0}
              }
            ],
            "terrainOptions": ["GrassHills"],
            "spawnPoint": {"x": 100.0, "y": 800.0}
          },
          "mainCharacter": {
            "gridSize": 20.0,
            "elementsAllowed": [],
            "maxSelectable": 9,
            "maxHP": 3.0,
            "cameraFollows": "vehicle"
          }
        }
        """);
    return levelFile.toString();
  }

  private <T> List<T> filterByType(List<GameObject> objects, Class<T> type) {
    List<T> result = new ArrayList<>();
    for (GameObject object : objects) {
      if (type.isInstance(object)) {
        result.add(type.cast(object));
      }
    }
    return result;
  }
}
