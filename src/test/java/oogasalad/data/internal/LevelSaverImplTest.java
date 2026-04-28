package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
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

  // ── gameMetadata section ─────────────────────────────────────────────────

  @Test
  void saveLevel_writesGameMetadataFromAttributes() throws IOException {
    LevelAttributes attrs = new LevelAttributes();
    attrs.setLevelName("Rolling Hills");
    attrs.setGameType("badpiggies");
    attrs.setGravity(-9.8);
    attrs.setDrag(0.01);
    attrs.setScreenWidth(3200);
    attrs.setScreenHeight(1600);

    File output = tempDir.resolve("level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), new FakeWorld(), attrs);

    JsonNode meta = new ObjectMapper().readTree(output).path("gameMetadata");
    assertEquals("Rolling Hills", meta.path("levelName").asText());
    assertEquals("badpiggies", meta.path("gameType").asText());
    assertEquals(-9.8, meta.path("gravity").asDouble(), 0.001);
    assertEquals(0.01, meta.path("drag").asDouble(), 0.0001);
    assertEquals(3200, meta.path("screenWidth").asInt());
    assertEquals(1600, meta.path("screenHeight").asInt());
  }

  // ── map section ──────────────────────────────────────────────────────────

  @Test
  void saveLevel_splineKnots_writtenToKnotLocations() throws IOException {
    FakeWorld world = new FakeWorld();
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(0, 10), new Vector(20, 15), new Vector(40, 10)), 100.0, 6);
    world.addObject(new TerrainSpline(spline, "grass.png", true, 0.6, "#4A2C0F"));

    File output = tempDir.resolve("level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, new LevelAttributes());

    JsonNode knots = new ObjectMapper().readTree(output).path("map").path("knotLocations");
    assertEquals(3, knots.size());
    assertEquals(0.0, knots.get(0).path("x").asDouble(), 0.001);
    assertEquals(10.0, knots.get(0).path("y").asDouble(), 0.001);
    assertEquals(40.0, knots.get(2).path("x").asDouble(), 0.001);
  }

  @Test
  void saveLevel_scaleFactor_appliedToKnotCoordinates() throws IOException {
    LevelSaverImpl scalingSaver = new LevelSaverImpl(20.0);
    FakeWorld world = new FakeWorld();
    ParabolaSpline spline = new ParabolaSpline(
        List.of(new Vector(1, 2), new Vector(5, 3)), 10.0, 4);
    world.addObject(new TerrainSpline(spline, "", false, 0.5, "#fff"));

    File output = tempDir.resolve("scaled.json").toFile();
    scalingSaver.saveLevel(output.getAbsolutePath(), world, new LevelAttributes());

    JsonNode knot = new ObjectMapper().readTree(output).path("map").path("knotLocations").get(0);
    assertEquals(20.0, knot.path("x").asDouble(), 0.001);
    assertEquals(40.0, knot.path("y").asDouble(), 0.001);
  }

  @Test
  void saveLevel_spawnPoint_writtenToMap() throws IOException {
    FakeWorld world = new FakeWorld();
    world.addObject(new SpawnPointComponent(17.5, 15.0));

    File output = tempDir.resolve("level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), world, new LevelAttributes());

    JsonNode spawn = new ObjectMapper().readTree(output).path("map").path("spawnPoint");
    assertEquals(17.5, spawn.path("x").asDouble(), 0.001);
    assertEquals(15.0, spawn.path("y").asDouble(), 0.001);
  }

  @Test
  void saveLevel_emptyWorld_writesEmptyKnotLocations() throws IOException {
    File output = tempDir.resolve("empty.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), new FakeWorld(), new LevelAttributes());

    JsonNode knots = new ObjectMapper().readTree(output).path("map").path("knotLocations");
    assertEquals(0, knots.size());
  }

  // ── mainCharacter section ────────────────────────────────────────────────

  @Test
  void saveLevel_writesMainCharacterFromAttributes() throws IOException {
    LevelAttributes attrs = new LevelAttributes();
    attrs.setGridSize(60);
    attrs.setElementsAllowed(List.of("body", "wheel", "motor"));
    attrs.setMaxSelectable(5);
    attrs.setMaxHP(100);

    File output = tempDir.resolve("level.json").toFile();
    saver.saveLevel(output.getAbsolutePath(), new FakeWorld(), attrs);

    JsonNode mc = new ObjectMapper().readTree(output).path("mainCharacter");
    assertEquals(60.0, mc.path("gridSize").asDouble(), 0.001);
    assertEquals(5, mc.path("maxSelectable").asInt());
    assertEquals(100.0, mc.path("maxHP").asDouble(), 0.001);
    assertEquals(3, mc.path("elementsAllowed").size());
    assertEquals("body", mc.path("elementsAllowed").get(0).asText());
  }

  // ── Error handling ───────────────────────────────────────────────────────

  @Test
  void saveLevel_invalidPath_throwsLevelSaveException() {
    assertThrows(LevelSaveException.class,
        () -> saver.saveLevel("/invalid:/path/level.json", new FakeWorld(), new LevelAttributes()));
  }

  @Test
  void constructor_zeroScale_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new LevelSaverImpl(0));
  }

  @Test
  void constructor_negativeScale_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new LevelSaverImpl(-1));
  }

  // ── Test doubles ─────────────────────────────────────────────────────────

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

    /**
     * Returns all registered dynamic components in registration order.
     */
    @Override
    public List<DynamicComponent> getActiveDynamicComponents() {
      return getActiveComponents().stream()
          .filter(DynamicComponent.class::isInstance)
          .map(DynamicComponent.class::cast)
          .toList();
    }

    @Override
    public List<Joint> getJoints() {
      return List.of();
    }

    @Override
    public void addComponent(GameComponent c) {
      components.add(c);
    }

    @Override
    public void removeComponent(GameComponent c) {
      components.remove(c);
    }

    @Override
    public void clear() {
      components.clear();
    }

    @Override
    public void addJoint(Joint j) {
    }

    @Override
    public void removeJoint(Joint j) {
    }

    @Override
    public double getDragCoefficient() {
      return 0.0;
    }

    int objectCount() {
      return components.size();
    }
  }
}
