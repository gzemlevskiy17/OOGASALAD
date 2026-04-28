package oogasalad.controller.player.internal;

import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelPaths;
import oogasalad.model.engine.api.World;

/**
 * Resets the simulation, reloads the level into the World, and starts again. Re-parses the
 * sibling {@code game.json} each call so the catalog is always fresh.
 *
 * <p>NOTE: World has no {@code clear()} method yet. Until it does, this action reloads objects
 * on top of existing ones.
 *
 * @author Paulina Vvedenskaya
 */
public class RestartAction implements Action {

  private final GameController gameController;
  private final LevelLoader levelLoader;
  private final GameLoader gameLoader;
  private final World world;
  private final String levelPath;

  public RestartAction(GameController gameController, LevelLoader levelLoader,
      GameLoader gameLoader, World world, String levelPath) {
    if (gameController == null) {
      throw new IllegalArgumentException("gameController must not be null");
    }
    if (levelLoader == null) {
      throw new IllegalArgumentException("levelLoader must not be null");
    }
    if (gameLoader == null) {
      throw new IllegalArgumentException("gameLoader must not be null");
    }
    if (levelPath == null) {
      throw new IllegalArgumentException("levelPath must not be null");
    }
    this.gameController = gameController;
    this.levelLoader = levelLoader;
    this.gameLoader = gameLoader;
    this.world = world;
    this.levelPath = levelPath;
  }

  @Override
  public void execute() {
    gameController.resetSimulation();
    LevelAttributes attributes = levelLoader.loadAttributes(levelPath);
    GameConfig game = gameLoader.loadGame(
        LevelPaths.gameFileForLevel(levelPath, attributes.getGameType()));
    levelLoader.loadLevel(levelPath, game, world);
    gameController.startSimulation();
  }
}
