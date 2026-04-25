package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.internal.TerminalResultHandler.StatsReceiver;
import oogasalad.data.api.LevelId;
import oogasalad.data.internal.LevelProgression;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;
import oogasalad.model.player.api.ScoreCalculator;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;
import oogasalad.model.player.internal.ScoreCalculatorImpl;
import oogasalad.model.player.internal.SqlitePlayHistoryRepo;
import oogasalad.model.player.internal.SqliteUserProgressionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration-style: real {@link ScoreCalculatorImpl} + real {@link SqlitePlayHistoryRepo} +
 * real {@link SqliteUserProgressionRepo} against in-memory SQLite. Stats receivers are lambdas
 * that record each call so tests can assert both the DB side and the UI-push side.
 */
class TerminalResultHandlerTest {

  private static final LevelId FOREST_FIRST = new LevelId("forest", "first");
  private static final LevelId FOREST_SECOND = new LevelId("forest", "second");
  private static final String FOREST_FIRST_PATH = "data/levels/forest/first.json";

  private PlayHistoryRepository repo;
  private UserProgressionRepository progressionRepo;
  private LevelProgression progression;
  private ScoreCalculator calculator;
  private List<Object[]> winCalls;
  private List<Object[]> loseCalls;
  private TerminalResultHandler handler;

  @BeforeEach
  void setUp() {
    repo = new SqlitePlayHistoryRepo("jdbc:sqlite::memory:");
    progressionRepo = new SqliteUserProgressionRepo("jdbc:sqlite::memory:");
    // A small canonical play order for the tests: forest/first → forest/second → forest/third.
    progression = new LevelProgression(Map.of(
        "forest", List.of("first", "second", "third")));
    calculator = new ScoreCalculatorImpl();
    winCalls = new ArrayList<>();
    loseCalls = new ArrayList<>();
    StatsReceiver winSink = (e, s, p, nt, ns, np) ->
        winCalls.add(new Object[] {e, s, p, nt, ns, np});
    StatsReceiver loseSink = (e, s, p, nt, ns, np) ->
        loseCalls.add(new Object[] {e, s, p, nt, ns, np});
    handler = new TerminalResultHandler(
        calculator, repo, progressionRepo, progression, () -> "alice", winSink, loseSink);
  }

  private GameplayScore winningStraightLineRun() {
    return new GameplayScore(
        FOREST_FIRST_PATH, 1000.0, 0.0, 1000.0, 1.0, 12.5, true, Instant.ofEpochMilli(42));
  }

  @Test
  void winningOutcome_persistsRow_andPushesToWinScreenOnly() {
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());

    assertEquals(1, winCalls.size());
    assertEquals(0, loseCalls.size());
    List<PlayHistoryEntry> saved = repo.findAllByUser("alice");
    assertEquals(1, saved.size());
    assertEquals(100.0, saved.get(0).score(), 1e-6);
    assertTrue(saved.get(0).won());
  }

  @Test
  void losingOutcome_displaysActualProgress_notOne() {
    GameplayScore loss = new GameplayScore(
        FOREST_FIRST_PATH, 500.0, 500.0, 1000.0, 0.5, 20.0, false, Instant.ofEpochMilli(10));

    handler.handle(Outcome.GAME_LOST, FOREST_FIRST, loss);

    assertEquals(0, winCalls.size());
    assertEquals(1, loseCalls.size());
    assertEquals(0.5, (double) loseCalls.get(0)[2], 1e-6);
  }

  @Test
  void nonTerminalOutcome_isNoOp() {
    handler.handle(Outcome.OPEN_PLAYER_ENTRY, FOREST_FIRST, winningStraightLineRun());

    assertTrue(winCalls.isEmpty());
    assertTrue(loseCalls.isEmpty());
    assertTrue(repo.findAllByUser("alice").isEmpty());
  }

  @Test
  void firstRunOnLevel_reportsAllThreeNewRecords() {
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());

    Object[] stats = winCalls.get(0);
    assertTrue((boolean) stats[3]);  // newTime
    assertTrue((boolean) stats[4]);  // newScore
    assertTrue((boolean) stats[5]);  // newProgress
  }

  @Test
  void slowerFollowUpRun_reportsNoNewRecords() {
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());
    winCalls.clear();
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, new GameplayScore(
        FOREST_FIRST_PATH, 1000.0, 0.0, 1000.0, 1.0, 20.0, true, Instant.ofEpochMilli(50)));

    Object[] stats = winCalls.get(0);
    assertFalse((boolean) stats[3]);
    assertFalse((boolean) stats[4]);
    assertFalse((boolean) stats[5]);
  }

  @Test
  void nullUserId_skipsPersistence_butStillPushesStats() {
    List<Object[]> anonWin = new ArrayList<>();
    TerminalResultHandler anon = new TerminalResultHandler(
        calculator, repo, progressionRepo, progression, () -> null,
        (e, s, p, nt, ns, np) -> anonWin.add(new Object[] {e, s, p, nt, ns, np}),
        (e, s, p, nt, ns, np) -> {});

    anon.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());

    assertEquals(1, anonWin.size());
    assertTrue(repo.findAllByUser("alice").isEmpty());
    assertTrue(progressionRepo.findByUser("alice").isEmpty());
  }

  @Test
  void winAdvancesFurthestReachedCursor_whenNoPriorProgression() {
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());

    UserProgression cursor = progressionRepo.findByUser("alice").orElseThrow();
    assertEquals(FOREST_FIRST, cursor.furthestReached());
  }

  @Test
  void winAdvancesFurthestReachedCursor_whenNewLevelBeatsCurrent() {
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());
    handler.handle(Outcome.GAME_WON, FOREST_SECOND, new GameplayScore(
        "data/levels/forest/second.json", 1000.0, 0.0, 1000.0, 1.0, 15.0, true,
        Instant.ofEpochMilli(100)));

    assertEquals(FOREST_SECOND,
        progressionRepo.findByUser("alice").orElseThrow().furthestReached());
  }

  @Test
  void winOnEarlierLevel_doesNotMoveTheCursorBackward() {
    handler.handle(Outcome.GAME_WON, FOREST_SECOND, new GameplayScore(
        "data/levels/forest/second.json", 1000.0, 0.0, 1000.0, 1.0, 15.0, true,
        Instant.ofEpochMilli(100)));
    handler.handle(Outcome.GAME_WON, FOREST_FIRST, winningStraightLineRun());

    assertEquals(FOREST_SECOND,
        progressionRepo.findByUser("alice").orElseThrow().furthestReached());
  }

  @Test
  void lossDoesNotAdvanceFurthestReached() {
    GameplayScore loss = new GameplayScore(
        FOREST_FIRST_PATH, 500.0, 500.0, 1000.0, 0.5, 20.0, false, Instant.ofEpochMilli(10));

    handler.handle(Outcome.GAME_LOST, FOREST_FIRST, loss);

    // History row is saved, but no progression cursor is created.
    assertEquals(1, repo.findAllByUser("alice").size());
    assertNull(progressionRepo.findByUser("alice")
        .map(UserProgression::furthestReached).orElse(null));
  }
}
