package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.internal.cheats.CheatInputHandler;
import oogasalad.controller.player.internal.cheats.CheatKeyBinding;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.internal.ScoreServiceImpl;
import org.junit.jupiter.api.Test;

/**
 * Tests keyboard input forwarding through a gameplay session.
 *
 * @author Emilio Garcia
 */
class GameplaySessionKeyInputTest {

  @Test
  void HandleKeyPress_KeyBoundToControllerAndCheatAction_ExecutesBothRoutes() {
    RecordingController controller = new RecordingController();
    int[] cheatExecutions = {0};
    GameplaySession session = new GameplaySession(controller,
        new CheatInputHandler(List.of(
            new CheatKeyBinding(Set.of(KeyCode.H), () -> cheatExecutions[0]++))));

    session.handleKeyPress(KeyCode.H);

    assertEquals(List.of(KeyCode.H), controller.pressedKeys,
        "Keyboard presses should reach the normal gameplay controller input route.");
    assertEquals(1, cheatExecutions[0],
        "Keyboard presses should also reach configured cheat chord tracking.");
  }

  @Test
  void HandleKeyRelease_CheatChordReleased_AllowsCheatActionToFireAgain() {
    RecordingController controller = new RecordingController();
    int[] cheatExecutions = {0};
    GameplaySession session = new GameplaySession(controller,
        new CheatInputHandler(List.of(
            new CheatKeyBinding(Set.of(KeyCode.SHIFT, KeyCode.H),
                () -> cheatExecutions[0]++))));

    session.handleKeyPress(KeyCode.SHIFT);
    session.handleKeyPress(KeyCode.H);
    session.handleKeyPress(KeyCode.H);
    session.handleKeyRelease(KeyCode.H);
    session.handleKeyPress(KeyCode.H);

    assertEquals(2, cheatExecutions[0],
        "Releasing a chord key should allow the cheat to fire again on the next completed chord.");
  }

  @Test
  void Pause_ActiveCheatChord_ClearsCheatPressedState() {
    RecordingController controller = new RecordingController();
    int[] cheatExecutions = {0};
    GameplaySession session = new GameplaySession(controller,
        new CheatInputHandler(List.of(
            new CheatKeyBinding(Set.of(KeyCode.SHIFT, KeyCode.H),
                () -> cheatExecutions[0]++))));

    session.handleKeyPress(KeyCode.SHIFT);
    session.pause();
    session.handleKeyPress(KeyCode.H);

    assertEquals(1, controller.pauseCalls,
        "Pausing should still forward to the gameplay controller.");
    assertEquals(0, cheatExecutions[0],
        "Pausing should clear held cheat keys so stale key state does not complete a chord.");
  }

  @Test
  void CurrentScore_SharedScoreServiceReflectsCheatAwards() {
    RecordingController controller = new RecordingController();
    ScoreServiceImpl scoreService = new ScoreServiceImpl();
    GameplaySession session = new GameplaySession(controller,
        new CheatInputHandler(List.of(
            new CheatKeyBinding(Set.of(KeyCode.M), () -> scoreService.addPoints(10)))),
        scoreService);

    session.handleKeyPress(KeyCode.M);

    assertEquals(10, session.currentScore(),
        "Gameplay sessions should expose the same score service used by cheat actions.");
  }

  private static final class RecordingController implements GameController {

    private final List<KeyCode> pressedKeys = new ArrayList<>();
    private int pauseCalls;

    @Override
    public void startSimulation() {
    }

    @Override
    public void pauseSimulation() {
      pauseCalls++;
    }

    @Override
    public void stopSimulation() {
    }

    @Override
    public void resetSimulation() {
    }

    @Override
    public void registerAction(String actionName, Action action) {
    }

    @Override
    public void setOnOutcome(Consumer<Outcome> callback) {
    }

    @Override
    public void setOnStatsTick(BiConsumer<Double, Double> callback) {
    }

    @Override
    public void setOnTerminalScore(Consumer<GameplayScore> callback) {
    }

    @Override
    public void handleButtonPress(String buttonId) {
    }

    @Override
    public void handleButtonRelease(String buttonId) {
    }

    @Override
    public void handleKeyPress(KeyCode key) {
      pressedKeys.add(key);
    }

    @Override
    public void completeRun(Outcome outcome) {
    }
  }
}
