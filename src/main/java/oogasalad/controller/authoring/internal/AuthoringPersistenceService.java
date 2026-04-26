package oogasalad.controller.authoring.internal;

import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.World;
import oogasalad.view.authoring.api.AuthoringCanvas;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @Author Paulina Vvedenskaya
 * <p>
 * Wraps save/load for the authoring environment. Owns the post-load housekeeping (ensuring a spawn
 * exists, clearing undo history, refreshing the canvas) so the controller's save/load methods are
 * one-liners.
 */
public class AuthoringPersistenceService {

  private static final Logger LOG = LogManager.getLogger(AuthoringPersistenceService.class);

  private final LevelLoader loader;
  private final LevelSaver saver;
  private final World world;
  private final LevelAttributes levelAttributes;
  private final SpawnPointService spawnPointService;
  private final CanvasActionHistory history;
  private final AuthoringCanvas canvas;

  public AuthoringPersistenceService(LevelLoader loader, LevelSaver saver, World world,
      LevelAttributes levelAttributes, SpawnPointService spawnPointService,
      CanvasActionHistory history, AuthoringCanvas canvas) {
    this.loader = loader;
    this.saver = saver;
    this.world = world;
    this.levelAttributes = levelAttributes;
    this.spawnPointService = spawnPointService;
    this.history = history;
    this.canvas = canvas;
  }

  public void save(String fileName) {
    saver.saveLevel(fileName, world, levelAttributes);
    LOG.info("Saved level to {}", fileName);
  }

  public void load(String fileName) {
    world.clear();
    loader.loadLevel(fileName, world, levelAttributes, null);
    spawnPointService.getSpawnPoint();
    history.clear();
    canvas.refreshGrid();
    LOG.info("Loaded level from {}", fileName);
  }
}
