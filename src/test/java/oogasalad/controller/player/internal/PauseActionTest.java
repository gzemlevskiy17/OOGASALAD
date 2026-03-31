package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import oogasalad.controller.player.api.GameController;

/**
 * Tests for PauseAction.
 *
 * Uses Mockito to create a fake GameController so we can verify
 * exactly which methods were called without writing a fake class.
 */
class PauseActionTest {

  private GameController controller;
  private PauseAction pauseAction;

  @BeforeEach
  void setUp() {
    controller = mock(GameController.class);
    pauseAction = new PauseAction(controller);
  }

  @Test
  void firstExecute_callsPauseSimulation() {
    pauseAction.execute();

    verify(controller).pauseSimulation();
  }

  @Test
  void secondExecute_callsStartSimulation() {
    pauseAction.execute();  // pause
    pauseAction.execute();  // resume

    verify(controller).pauseSimulation();
    verify(controller).startSimulation();
  }

  @Test
  void fourExecutes_alternatesTwiceEach() {
    pauseAction.execute();
    pauseAction.execute();
    pauseAction.execute();
    pauseAction.execute();

    verify(controller, times(2)).pauseSimulation();
    verify(controller, times(2)).startSimulation();
  }

}
