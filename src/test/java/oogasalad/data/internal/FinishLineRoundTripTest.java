package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that finish line, heal, and damage static components survive the full
 * save-to-catalog, place-in-level, load-from-level round trip.
 *
 * @author Paulina Vvedenskaya
 */
class FinishLineRoundTripTest {

  @TempDir
  Path tempDir;

  private GameCatalogSaver catalogSaver;
  private LevelLoaderImpl levelLoader;
  private StubWorld world;
  private LevelAttributes attrs;

  @BeforeEach
  void setUp() {
    catalogSaver = new GameCatalogSaver();
    levelLoader = new LevelLoaderImpl();
    world = new StubWorld();
    attrs = new LevelAttributes();
  }

  @Test
  void finishLineInCatalog_loadedAsGoalLineComponent() throws IOException {
    GameConfig config = saveCatalogWithFinishLine();
    File level = writeLevelWithObject("GoalLine", 300, 400);

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    long goalLines = world.components.stream()
        .filter(c -> c instanceof GoalLineComponent)
        .count();
    assertEquals(1, goalLines, "Expected exactly one GoalLineComponent in the world");
  }

  @Test
  void finishLinePosition_matchesLevelJson() throws IOException {
    GameConfig config = saveCatalogWithFinishLine();
    File level = writeLevelWithObject("GoalLine", 500, 200);

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    GoalLineComponent goal = world.components.stream()
        .filter(c -> c instanceof GoalLineComponent)
        .map(c -> (GoalLineComponent) c)
        .findFirst()
        .orElseThrow();
    assertEquals(500.0, goal.x(), 0.001);
    assertEquals(200.0, goal.y(), 0.001);
  }

  @Test
  void healObjectInCatalog_loadedAsPickupComponent() throws IOException {
    GameConfig config = saveCatalogWithHealAndDamage();
    File level = writeLevelWithObject("Heart", 100, 100);

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    long pickups = world.components.stream()
        .filter(c -> c instanceof PickupComponent)
        .count();
    assertEquals(1, pickups, "Expected one PickupComponent for the heal object");
  }

  @Test
  void damageObjectInCatalog_loadedAsPickupComponent() throws IOException {
    GameConfig config = saveCatalogWithHealAndDamage();
    File level = writeLevelWithObject("Bomb", 200, 200);

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    long pickups = world.components.stream()
        .filter(c -> c instanceof PickupComponent)
        .count();
    assertEquals(1, pickups, "Expected one PickupComponent for the damage object");
  }

  @Test
  void multipleStaticObjects_allLoadCorrectly() throws IOException {
    GameConfig config = saveCatalogWithFinishLine();
    File level = writeLevelWithObjects(
        List.of("GoalLine", "GoalLine"),
        List.of(100.0, 500.0),
        List.of(200.0, 200.0));

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    long goalLines = world.components.stream()
        .filter(c -> c instanceof GoalLineComponent)
        .count();
    assertEquals(2, goalLines, "Expected two GoalLineComponents");
  }

  @Test
  void unknownObjectType_skippedWithoutCrash() throws IOException {
    GameConfig config = saveCatalogWithFinishLine();
    File level = writeLevelWithObject("NonExistentType", 100, 100);

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    assertTrue(world.components.isEmpty(), "Unknown type should be skipped");
  }

  @Test
  void mixOfFinishLineAndPickups_allLoadedCorrectly() throws IOException {
    File catalogFile = tempDir.resolve("game.json").toFile();
    var body = new ComponentInfo("body", "body.png", 1.0, List.of());
    var finish = new StaticComponentInfo("GoalLine", "", 0,
        new CollisionAction("finish", 0));
    var heart = new StaticComponentInfo("Heart", "heart.png", 0,
        new CollisionAction("heal", 25));
    var bomb = new StaticComponentInfo("Bomb", "bomb.png", 0,
        new CollisionAction("damage", 30));
    catalogSaver.save(catalogFile.getAbsolutePath(), "testgame", "body",
        List.of(body), List.of(finish, heart, bomb), List.of());
    GameConfig config = new GameConfigImpl(catalogFile.getAbsolutePath());

    File level = writeLevelWithObjects(
        List.of("GoalLine", "Heart", "Bomb"),
        List.of(100.0, 200.0, 300.0),
        List.of(50.0, 50.0, 50.0));

    levelLoader.loadLevel(level.getAbsolutePath(), config, world, attrs);

    long goals = world.components.stream()
        .filter(c -> c instanceof GoalLineComponent).count();
    long pickups = world.components.stream()
        .filter(c -> c instanceof PickupComponent).count();
    assertEquals(1, goals, "Expected one GoalLineComponent");
    assertEquals(2, pickups, "Expected two PickupComponents (heart + bomb)");
  }

  private GameConfig saveCatalogWithFinishLine() throws IOException {
    File catalogFile = tempDir.resolve("game.json").toFile();
    var body = new ComponentInfo("body", "body.png", 1.0, List.of());
    var finish = new StaticComponentInfo("GoalLine", "", 0,
        new CollisionAction("finish", 0));
    catalogSaver.save(catalogFile.getAbsolutePath(), "testgame", "body",
        List.of(body), List.of(finish), List.of());
    return new GameConfigImpl(catalogFile.getAbsolutePath());
  }

  private GameConfig saveCatalogWithHealAndDamage() throws IOException {
    File catalogFile = tempDir.resolve("game.json").toFile();
    var body = new ComponentInfo("body", "body.png", 1.0, List.of());
    var heart = new StaticComponentInfo("Heart", "heart.png", 0,
        new CollisionAction("heal", 25));
    var bomb = new StaticComponentInfo("Bomb", "bomb.png", 0,
        new CollisionAction("damage", 30));
    catalogSaver.save(catalogFile.getAbsolutePath(), "testgame", "body",
        List.of(body), List.of(heart, bomb), List.of());
    return new GameConfigImpl(catalogFile.getAbsolutePath());
  }

  private File writeLevelWithObject(String type, double x, double y) throws IOException {
    return writeLevelWithObjects(List.of(type), List.of(x), List.of(y));
  }

  private File writeLevelWithObjects(List<String> types, List<Double> xs, List<Double> ys)
      throws IOException {
    StringBuilder objects = new StringBuilder();
    for (int i = 0; i < types.size(); i++) {
      if (i > 0) {
        objects.append(",");
      }
      objects.append(String.format(
          "{\"type\":\"%s\",\"position\":{\"x\":%s,\"y\":%s}}",
          types.get(i), xs.get(i), ys.get(i)));
    }
    String json = String.format("""
        {
          "gameMetadata": {"levelName":"test","gameType":"testgame"},
          "map": {"objects": [%s]},
          "mainCharacter": {"gridSize":60,"elementsAllowed":["body"],"maxSelectable":5,"maxHP":100}
        }
        """, objects);
    File file = tempDir.resolve("level.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }

  private static class StubWorld implements World {

    final List<GameComponent> components = new ArrayList<>();

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
    public List<DynamicComponent> getActiveDynamicComponents() {
      return List.of();
    }

    @Override
    public List<Joint> getJoints() {
      return List.of();
    }

    @Override
    public void addComponent(GameComponent component) {
      components.add(component);
    }

    @Override
    public void removeComponent(GameComponent component) {
      components.remove(component);
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
  }
}
