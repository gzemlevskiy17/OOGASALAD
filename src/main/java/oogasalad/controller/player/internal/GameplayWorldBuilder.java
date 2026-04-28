package oogasalad.controller.player.internal;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.data.internal.LevelPaths;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.RigidBody;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;

/**
 * Builds the loaded gameplay world and inserts the player vehicle at the level spawn point.
 *
 * @author Cody
 */
final class GameplayWorldBuilder {

  private static final Logger LOG = LogManager.getLogger(GameplayWorldBuilder.class);
  private static final double DEFAULT_VIEW_WIDTH = 1280.0;
  private static final double DEFAULT_VIEW_HEIGHT = 720.0;
  private static final double BOUNDS_PADDING = 400.0;
  private static final double FALLBACK_WORLD_WIDTH = 2600.0;
  private static final double FALLBACK_WORLD_HEIGHT = 1600.0;
  private static final double DEFAULT_SPAWN_X = 380.0;
  private static final double DEFAULT_SPAWN_Y = 330.0;

  // Keep the world at least one viewport plus padding so camera clamping and fall-loss checks
  // still behave when an authored level has only a few blocks.
  private static final double MIN_WORLD_WIDTH = DEFAULT_VIEW_WIDTH + BOUNDS_PADDING;
  private static final double MIN_WORLD_HEIGHT = DEFAULT_VIEW_HEIGHT + BOUNDS_PADDING;

  private final VehicleFactory vehicleFactory = new VehicleFactory();
  private final LevelLoader levelLoader;
  private final GameLoader gameLoader;
  private final Supplier<String> levelPathSupplier;

  GameplayWorldBuilder(LevelLoader levelLoader, GameLoader gameLoader,
      Supplier<String> levelPathSupplier) {
    this.levelLoader = levelLoader;
    this.gameLoader = gameLoader;
    this.levelPathSupplier = levelPathSupplier;
  }

  BuiltWorld build(VehicleLayout layout) {
    World world = new WorldImpl();
    LoadedLevel loadedLevel = loadLevelInto(world);
    LevelBounds bounds = computeLevelBounds(world);
    SpawnPointComponent spawn = findSpawnPoint(world);
    Map<PlacedComponent, RigidBody> placements = vehicleFactory.buildVehicle(
        layout, world, spawnX(spawn), spawnY(spawn));
    return new BuiltWorld(world, bounds, placements, loadedLevel.gameConfig(),
        loadedLevel.attributes());
  }

  private LoadedLevel loadLevelInto(World world) {
    String levelPath = levelPathSupplier.get();
    if (levelPath == null || levelPath.isBlank()) {
      return new LoadedLevel(null, new LevelAttributes());
    }
    LevelAttributes attributes = levelLoader.loadAttributes(levelPath);
    GameConfig gameConfig = loadGameConfig(levelPath, attributes);
    levelLoader.loadLevel(levelPath, gameConfig, world);
    if (gameConfig != null) {
      vehicleFactory.getRegistry().registerFromConfig(gameConfig);
    }
    return new LoadedLevel(gameConfig, attributes);
  }

  private GameConfig loadGameConfig(String levelPath, LevelAttributes attributes) {
    try {
      return gameLoader.loadGame(LevelPaths.gameFileForLevel(levelPath, attributes.getGameType()));
    } catch (RuntimeException e) {
      LOG.warn("Game config not found for level {}; running without catalog: {}", levelPath,
          e.getMessage());
      return null;
    }
  }

  private SpawnPointComponent findSpawnPoint(World world) {
    for (GameComponent component : world.getAllComponents()) {
      if (component instanceof SpawnPointComponent spawnPoint) {
        return spawnPoint;
      }
    }
    return null;
  }

  private double spawnX(SpawnPointComponent spawn) {
    if (spawn == null) {
      return DEFAULT_SPAWN_X;
    }
    return spawn.x();
  }

  private double spawnY(SpawnPointComponent spawn) {
    if (spawn == null) {
      return DEFAULT_SPAWN_Y;
    }
    return spawn.y();
  }

  private LevelBounds computeLevelBounds(World world) {
    double maxX = 0;
    double maxY = 0;
    for (GameComponent component : world.getAllComponents()) {
      switch (component) {
        case TerrainBlock terrainBlock -> {
          maxX = Math.max(maxX, terrainBlock.x() + terrainBlock.getWidth());
          maxY = Math.max(maxY, terrainBlock.y() + terrainBlock.getHeight());
        }
        case TerrainSpline terrainSpline -> {
          maxX = Math.max(maxX, terrainSpline.x() + terrainSpline.getWidth());
          maxY = Math.max(maxY, terrainSpline.getSpline().baselineY());
        }
        case GoalLineComponent goalLine -> {
          maxX = Math.max(maxX, goalLine.x() + goalLine.getWidth());
          maxY = Math.max(maxY, goalLine.y() + goalLine.getHeight());
        }
        default -> {
        }
      }
    }
    return paddedBounds(maxX, maxY);
  }

  private LevelBounds paddedBounds(double maxX, double maxY) {
    double preferredWidth = FALLBACK_WORLD_WIDTH;
    if (maxX > 0) {
      preferredWidth = maxX + BOUNDS_PADDING;
    }
    double preferredHeight = FALLBACK_WORLD_HEIGHT;
    if (maxY > 0) {
      preferredHeight = maxY + BOUNDS_PADDING;
    }
    double width = Math.max(MIN_WORLD_WIDTH, preferredWidth);
    double height = Math.max(MIN_WORLD_HEIGHT, preferredHeight);
    return new LevelBounds(width, height);
  }

  record LevelBounds(double width, double height) {
  }

  private record LoadedLevel(GameConfig gameConfig, LevelAttributes attributes) {
  }

  record BuiltWorld(World world, LevelBounds bounds, Map<PlacedComponent, RigidBody> placements,
                    GameConfig gameConfig, LevelAttributes levelAttributes) {
  }
}
