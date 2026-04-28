package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oogasalad.controller.player.api.GameController;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.model.engine.api.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests for RestartAction.
 */
class RestartActionTest {

  private static final String LEVEL_PATH = "levels/level_01.json";

  private GameController controller;
  private LevelLoader levelLoader;
  private World world;
  private GameConfig config;
  private RestartAction restartAction;
  private GameLoader gameLoader;
  private LevelAttributes attributes;

  @BeforeEach
  void setUp() {
    controller  = mock(GameController.class);
    levelLoader = mock(LevelLoader.class);
    world       = mock(World.class);
    config      = mock(GameConfig.class);
    gameLoader = mock(GameLoader.class);
    attributes = new LevelAttributes();
    attributes.setGameType("badpiggies");
    when(levelLoader.loadAttributes(anyString())).thenReturn(attributes);
    when(gameLoader.loadGame(anyString())).thenReturn(config);
    restartAction = new RestartAction(controller, levelLoader, gameLoader, world, LEVEL_PATH);
  }

  @Test
  void execute_callsResetThenLoadThenStart_inOrder() {
    restartAction.execute();

    InOrder order = inOrder(controller, levelLoader);
    order.verify(controller).resetSimulation();
    order.verify(levelLoader).loadLevel(anyString(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
    order.verify(controller).startSimulation();
  }

  @Test
  void execute_loadsAttributes() {
    restartAction.execute();

    verify(levelLoader).loadAttributes(LEVEL_PATH);
  }

  @Test
  void constructor_nullController_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(null, levelLoader, gameLoader, world, LEVEL_PATH));
  }

  @Test
  void constructor_nullLevelLoader_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(controller, null, gameLoader, world, LEVEL_PATH));
  }

  @Test
  void constructor_nullLevelPath_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new RestartAction(controller, levelLoader, gameLoader, world, null));
  }
}
