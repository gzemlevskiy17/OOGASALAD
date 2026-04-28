package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.GameplayStatsTracker;
import oogasalad.view.core.internal.CameraImpl;
import org.junit.jupiter.api.Test;

/**
 * Tests controller terminal completion used by cheat key effects.
 *
 * @author Emilio Garcia
 */
class GameControllerImplCheatCompletionTest {

  @Test
  void CompleteRun_GameWon_FinalizesScoreBeforeOutcome() {
    RecordingStatsTracker tracker = new RecordingStatsTracker();
    GameControllerImpl controller = controllerWith(tracker);
    List<String> eventOrder = new ArrayList<>();
    List<Outcome> outcomes = new ArrayList<>();
    controller.setOnTerminalScore(score -> eventOrder.add("score"));
    controller.setOnOutcome(outcome -> {
      eventOrder.add("outcome");
      outcomes.add(outcome);
    });

    controller.completeRun(Outcome.GAME_WON);

    assertEquals(List.of(true), tracker.wonValues,
        "Completing a win cheat should finalize the run as a win.");
    assertEquals(List.of("score", "outcome"), eventOrder,
        "Terminal score should be available before screen navigation receives the outcome.");
    assertEquals(List.of(Outcome.GAME_WON), outcomes,
        "Completing a win cheat should emit the normal GAME_WON outcome.");
  }

  @Test
  void CompleteRun_NonTerminalOutcome_DoesNotFinalizeScore() {
    RecordingStatsTracker tracker = new RecordingStatsTracker();
    GameControllerImpl controller = controllerWith(tracker);
    List<Outcome> outcomes = new ArrayList<>();
    controller.setOnOutcome(outcomes::add);

    controller.completeRun(Outcome.BACK_TO_MENU);

    assertTrue(tracker.wonValues.isEmpty(),
        "Non-terminal outcomes should not finalize gameplay scoring through completeRun.");
    assertTrue(outcomes.isEmpty(),
        "Non-terminal outcomes should not be emitted through the cheat completion path.");
  }

  @Test
  void CompleteRun_RunAlreadyCompleted_IgnoresLaterTerminalOutcome() {
    RecordingStatsTracker tracker = new RecordingStatsTracker();
    GameControllerImpl controller = controllerWith(tracker);
    List<Outcome> outcomes = new ArrayList<>();
    controller.setOnOutcome(outcomes::add);

    controller.completeRun(Outcome.GAME_WON);
    controller.completeRun(Outcome.GAME_LOST);

    assertEquals(List.of(true), tracker.wonValues,
        "Only the first terminal cheat completion should finalize gameplay scoring.");
    assertEquals(List.of(Outcome.GAME_WON), outcomes,
        "Only the first terminal cheat completion should emit an outcome.");
  }

  private GameControllerImpl controllerWith(RecordingStatsTracker tracker) {
    return new GameControllerImpl(
        new NoOpEngine(),
        new WorldImpl(),
        (camera, objects) -> {
        },
        new CameraImpl(0, 0, 100, 100),
        camera -> {
        },
        world -> false,
        world -> false,
        new InputRouterImpl(),
        GameConfigImpl.empty(),
        tracker);
  }

  private static final class NoOpEngine implements GameEngine {

    @Override
    public void step(double elapsedTime) {
    }

    @Override
    public void setGravity(Vector gravityVector) {
    }
  }

  private static final class RecordingStatsTracker implements GameplayStatsTracker {

    private final List<Boolean> wonValues = new ArrayList<>();

    @Override
    public void sample(World world, double dtSeconds) {
    }

    @Override
    public double currentDistanceTraveled() {
      return 0;
    }

    @Override
    public double currentElapsedSeconds() {
      return 0;
    }

    @Override
    public double currentProgressPercent() {
      return 0;
    }

    @Override
    public GameplayScore finish(boolean won) {
      wonValues.add(won);
      return new GameplayScore("level", 0, 0, 0, 0, 0, won, Instant.EPOCH);
    }
  }
}
