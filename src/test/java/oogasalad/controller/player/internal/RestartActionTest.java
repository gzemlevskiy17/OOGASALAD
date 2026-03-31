package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import oogasalad.controller.player.api.GameController;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.api.World;

/**
 * Tests for RestartAction.
 *
 */
class RestartActionTest {

  private static final String LEVEL_PATH = "levels/level_01.json";

  private GameController controller;
  private LevelLoader levelLoader;
  private World world;
  private GameConfig config;
  private RestartAction restartAction;

  @BeforeEach
  void setUp() {
    controller  = mock(GameController.class);
    levelLoader = mock(LevelLoader.class);
    world       = mock(World.class);
    config      = mock(GameConfig.class);
    restartAction = new RestartAction(controller, levelLoader, world, config, LEVEL_PATH);
  }

  @Test
  void execute_callsStopThenLoadThenStart_inOrder() {
    restartAction.execute();

    InOrder order = inOrder(controller, levelLoader);
    order.verify(controller).stopSimulation();
    order.verify(levelLoader).loadLevel(LEVEL_PATH, world, config);
    order.verify(controller).startSimulation();
  }

  @Test
  void execute_loadsCorrectLevelPath() {
    restartAction.execute();

    verify(levelLoader).loadLevel(LEVEL_PATH, world, config);
  }

  @Test
  void constructor_nullController_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(null, levelLoader, world, config, LEVEL_PATH));
  }

  @Test
  void constructor_nullLevelLoader_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(controller, null, world, config, LEVEL_PATH));
  }

  @Test
  void constructor_nullLevelPath_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(controller, levelLoader, world, config, null));
  }
}
