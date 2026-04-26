package oogasalad.controller.authoring.internal;

import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.data.internal.LevelPaths;
import oogasalad.model.engine.api.World;
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
    GameConfig game = gameLoader.loadGame(LevelPaths.siblingGameFile(fileName));
    levelLoader.loadLevel(fileName, game, world, levelAttributes);
    spawnPointService.getSpawnPoint();
    history.clear();
    canvas.refreshGrid();
    LOG.info("Loaded level from {}", fileName);
  }
}
