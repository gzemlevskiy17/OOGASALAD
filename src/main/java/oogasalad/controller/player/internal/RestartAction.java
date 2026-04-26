package oogasalad.controller.player.internal;

import java.nio.file.Path;
import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.model.engine.api.World;

/**
 * Resets the simulation, reloads the level into the World, and starts again. Re-parses the
 * sibling game.json each call so the catalog is always fresh.
 *
 * <p>NOTE: World has no {@code clear()} method yet. Until it does, this action reloads objects
 * on top of existing ones.
 */
public class RestartAction implements Action {

  private static final String GAME_FILE_NAME = "game.json";

  private final GameController gameController;
  private final LevelLoader levelLoader;
  private final World world;
  private final String levelPath;

  public RestartAction(GameController gameController, LevelLoader levelLoader,
      World world, String levelPath) {
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
    this.levelPath = levelPath;
  }

  private static String siblingGamePath(String levelPath) {
    Path parent = Path.of(levelPath).getParent();
    return parent == null ? GAME_FILE_NAME : parent.resolve(GAME_FILE_NAME).toString();
  }

  @Override
  public void execute() {
    gameController.resetSimulation();
    GameLoader gameLoader = new GameLoaderImpl();
    GameConfig game = gameLoader.loadGame(siblingGamePath(levelPath));
    levelLoader.loadLevel(levelPath, game, world);
    gameController.startSimulation();
  }
}
