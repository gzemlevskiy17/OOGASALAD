package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.api.World;

/**
 * Resets the simulation, reloads the level into the World, and starts again.
 *
 * NOTE: World has no clear() method yet. Until it does, this action reloads
 * objects on top of existing ones. The team should add World.clear() so
 * loadLevel() starts from a clean state.
 */
public class RestartAction implements Action {

  private final GameController gameController;
  private final LevelLoader levelLoader;
  private final World world;
  private final GameConfig config;
  private final String levelPath;

  public RestartAction(GameController gameController, LevelLoader levelLoader,
      World world, GameConfig config, String levelPath) {
    if (gameController == null) {
      throw new IllegalArgumentException("gameController must not be null");
    }
    if (levelLoader == null) {
      throw new IllegalArgumentException("levelLoader must not be null");
    }
    if (levelPath == null) {
      throw new IllegalArgumentException("levelPath must not be null");
    }
    this.gameController = gameController;
    this.levelLoader = levelLoader;
    this.world = world;
    this.config = config;
    this.levelPath = levelPath;
  }

  @Override
  public void execute() {
    gameController.resetSimulation();
    // TODO: call world.clear() once that method is added to the World interface
    levelLoader.loadLevel(levelPath, world, config);
    gameController.startSimulation();
  }
}
