package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.data.internal.LevelPaths;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wraps save/load for the authoring environment. Owns the post-load housekeeping (ensuring a
 * spawn exists, clearing undo history, refreshing the canvas) so the controller's save/load
 * methods are one-liners. On load, the matching {@code game.json} is parsed from the level
 * file's parent directory before the level itself is loaded.
 *
 * @author Paulina Vvedenskaya
 */
public class AuthoringPersistenceService {

  private static final Logger LOG = LogManager.getLogger(AuthoringPersistenceService.class);
  private static final double DEFAULT_STATIC_SIZE_PIXELS = 60.0;
  private static final String DEFAULT_STATIC_COLOR = "#F5C542";

  private final LevelLoader levelLoader;
  private final LevelSaver levelSaver;
  private final GameLoader gameLoader;
  private final World world;
  private final LevelAttributes levelAttributes;
  private final SpawnPointService spawnPointService;
  private final CanvasActionHistory history;
  private final AuthoringCanvas canvas;

  public AuthoringPersistenceService(LevelLoader levelLoader, LevelSaver levelSaver,
      GameLoader gameLoader, World world, LevelAttributes levelAttributes,
      SpawnPointService spawnPointService, CanvasActionHistory history, AuthoringCanvas canvas) {
    this.levelLoader = levelLoader;
    this.levelSaver = levelSaver;
    this.gameLoader = gameLoader;
    this.world = world;
    this.levelAttributes = levelAttributes;
    this.spawnPointService = spawnPointService;
    this.history = history;
    this.canvas = canvas;
  }

  public void save(String fileName) {
    levelSaver.saveLevel(fileName, world, levelAttributes);
    LOG.info("Saved level to {}", fileName);
  }

  public void load(String fileName) {
    world.clear();
    LevelAttributes attributes = levelLoader.loadAttributes(fileName);
    GameConfig game = gameLoader.loadGame(
        LevelPaths.gameFileForLevel(fileName, attributes.getGameType()));
    levelLoader.loadLevel(fileName, game, world, levelAttributes);
    convertLoadedWorldToAuthoringSpace(game);
    spawnPointService.getSpawnPoint();
    history.clear();
    canvas.refreshGrid();
    LOG.info("Loaded level from {}", fileName);
  }

  private void convertLoadedWorldToAuthoringSpace(GameConfig game) {
    double scale = canvas.getCellSize();
    List<GameComponent> converted = new ArrayList<>();
    for (GameObject object : world.getObjects()) {
      if (object instanceof TerrainSpline spline) {
        converted.add(scaleTerrainSpline(spline, scale));
      } else if (object instanceof SpawnPointComponent spawn) {
        converted.add(scaleSpawnPoint(spawn, scale));
      } else if (object instanceof GoalLineComponent goalLine) {
        converted.add(toAuthoringStatic(goalLine.getComponentType(), goalLine.x(), goalLine.y(),
            scale, game));
      } else if (object instanceof PickupComponent pickup) {
        converted.add(toAuthoringStatic(pickup.getComponentType(), pickup.x(), pickup.y(), scale,
            game));
      } else if (object instanceof GameComponent component) {
        converted.add(component);
      }
    }
    world.clear();
    for (GameComponent component : converted) {
      world.addComponent(component);
    }
  }

  private TerrainSpline scaleTerrainSpline(TerrainSpline spline, double scale) {
    List<Vector> scaledKnots = spline.getSpline().knots().stream()
        .map(knot -> new Vector(knot.x() / scale, knot.y() / scale))
        .toList();
    double scaledBaseline = spline.getSpline().baselineY() / scale;
    ParabolaSpline scaledSpline = new ParabolaSpline(scaledKnots, scaledBaseline);
    return new TerrainSpline(spline.getDisplayName(), scaledSpline, spline.getSprite(),
        spline.isTiled(), spline.getFriction(), spline.getColor());
  }

  private SpawnPointComponent scaleSpawnPoint(SpawnPointComponent spawn, double scale) {
    return new SpawnPointComponent(spawn.x() / scale, spawn.y() / scale);
  }

  private AuthoringStaticComponent toAuthoringStatic(String typeId, double centerX, double centerY,
      double scale, GameConfig game) {
    double sizeInCells = DEFAULT_STATIC_SIZE_PIXELS / scale;
    double topLeftX = centerX / scale - sizeInCells / 2.0;
    double topLeftY = centerY / scale - sizeInCells / 2.0;
    String sprite = lookupStaticSprite(typeId, game);
    return new AuthoringStaticComponent(
        topLeftX, topLeftY, sizeInCells, sizeInCells, typeId, DEFAULT_STATIC_COLOR, sprite);
  }

  private String lookupStaticSprite(String typeId, GameConfig game) {
    if (game == null || typeId == null || typeId.isBlank()) {
      return "";
    }
    return game.staticComponents().stream()
        .filter(info -> typeId.equals(info.id()))
        .findFirst()
        .map(info -> info.spriteUrl() == null ? "" : info.spriteUrl())
        .orElse("");
  }
}
