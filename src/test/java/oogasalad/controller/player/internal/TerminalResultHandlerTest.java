package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.internal.TerminalResultHandler.StatsReceiver;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;
import oogasalad.model.player.api.ScoreCalculator;
import oogasalad.model.player.internal.ScoreCalculatorImpl;
import oogasalad.model.player.internal.SqlitePlayHistoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration-style: real {@link ScoreCalculatorImpl} + real {@link SqlitePlayHistoryRepo}
 * against in-memory SQLite. Stats receivers are lambdas that record each call so tests can
 * assert both the DB side and the UI-push side of the handler's work.
 */
class TerminalResultHandlerTest {

  private PlayHistoryRepository repo;
  private ScoreCalculator calculator;
  private List<Object[]> winCalls;
  private List<Object[]> loseCalls;
  private TerminalResultHandler handler;

  @BeforeEach
  void setUp() {
    repo = new SqlitePlayHistoryRepo("jdbc:sqlite::memory:");
    calculator = new ScoreCalculatorImpl();
    winCalls = new ArrayList<>();
    loseCalls = new ArrayList<>();
    StatsReceiver winSink = (e, s, p, nt, ns, np) ->
        winCalls.add(new Object[] {e, s, p, nt, ns, np});
    StatsReceiver loseSink = (e, s, p, nt, ns, np) ->
        loseCalls.add(new Object[] {e, s, p, nt, ns, np});
    handler = new TerminalResultHandler(calculator, repo, () -> "alice", winSink, loseSink);
  }

  private GameplayScore winningStraightLineRun() {
    return new GameplayScore(
        "level-1", 1000.0, 0.0, 1000.0, 1.0, 12.5, true, Instant.ofEpochMilli(42));
  }

  @Test
  void winningOutcome_persistsRow_andPushesToWinScreenOnly() {
    handler.handle(Outcome.GAME_WON, winningStraightLineRun());

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
        "level-1", 500.0, 500.0, 1000.0, 0.5, 20.0, false, Instant.ofEpochMilli(10));

    handler.handle(Outcome.GAME_LOST, loss);

    assertEquals(0, winCalls.size());
    assertEquals(1, loseCalls.size());
    assertEquals(0.5, (double) loseCalls.get(0)[2], 1e-6);
  }

  @Test
  void nonTerminalOutcome_isNoOp() {
    handler.handle(Outcome.OPEN_PLAYER_ENTRY, winningStraightLineRun());

    assertTrue(winCalls.isEmpty());
    assertTrue(loseCalls.isEmpty());
    assertTrue(repo.findAllByUser("alice").isEmpty());
  }

  @Test
  void firstRunOnLevel_reportsAllThreeNewRecords() {
    handler.handle(Outcome.GAME_WON, winningStraightLineRun());

    Object[] stats = winCalls.get(0);
    assertTrue((boolean) stats[3]);  // newTime
    assertTrue((boolean) stats[4]);  // newScore
    assertTrue((boolean) stats[5]);  // newProgress
  }

  @Test
  void slowerFollowUpRun_reportsNoNewRecords() {
    handler.handle(Outcome.GAME_WON, winningStraightLineRun());       // t=12.5, score=100
    winCalls.clear();
    handler.handle(Outcome.GAME_WON, new GameplayScore(               // t=20, score=100
        "level-1", 1000.0, 0.0, 1000.0, 1.0, 20.0, true, Instant.ofEpochMilli(50)));

    Object[] stats = winCalls.get(0);
    assertFalse((boolean) stats[3]);  // newTime — slower
    assertFalse((boolean) stats[4]);  // newScore — tied, not greater
    assertFalse((boolean) stats[5]);  // newProgress — tied
  }

  @Test
  void nullUserId_skipsPersistence_butStillPushesStats() {
    List<Object[]> anonWin = new ArrayList<>();
    TerminalResultHandler anon = new TerminalResultHandler(
        calculator, repo, () -> null,
        (e, s, p, nt, ns, np) -> anonWin.add(new Object[] {e, s, p, nt, ns, np}),
        (e, s, p, nt, ns, np) -> {});

    anon.handle(Outcome.GAME_WON, winningStraightLineRun());

    assertEquals(1, anonWin.size());
    assertTrue(repo.findAllByUser("alice").isEmpty());
  }
}
