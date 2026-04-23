package oogasalad.controller.player.internal;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;
import oogasalad.model.player.api.ScoreCalculator;

/**
 * Handles what happens at the end of a gameplay run: computes the numeric score, checks whether
 * this run beat the user's previous bests for that level, pushes the resulting stats (with
 * record flags) into the appropriate result screen, and persists a new play-history row.
 *
 * <p>Pure orchestration — no UI construction, no navigation, no auth knowledge beyond a
 * username supplier. No-ops safely for non-terminal outcomes or when no gameplay score is
 * available.
 */
public class TerminalResultHandler {

  /**
   * Six-argument sink for run stats. Both the win and lose result screens supply one of these
   * via method reference ({@code winScreen::setStats}), so this handler can stay unaware of the
   * concrete screen classes and remain unit-testable without a JavaFX runtime.
   */
  @FunctionalInterface
  public interface StatsReceiver {
    void setStats(double elapsedSeconds, double score, double progressPercent,
                  boolean isNewTime, boolean isNewScore, boolean isNewProgress);
  }

  private final ScoreCalculator calculator;
  private final PlayHistoryRepository repository;
  private final Supplier<String> userIdSupplier;
  private final StatsReceiver winReceiver;
  private final StatsReceiver loseReceiver;

  public TerminalResultHandler(ScoreCalculator calculator, PlayHistoryRepository repository,
      Supplier<String> userIdSupplier, StatsReceiver winReceiver, StatsReceiver loseReceiver) {
    this.calculator = calculator;
    this.repository = repository;
    this.userIdSupplier = userIdSupplier;
    this.winReceiver = winReceiver;
    this.loseReceiver = loseReceiver;
  }

  /** Terminal-outcome entry point. No-op for non-GAME_WON/GAME_LOST outcomes or null scores. */
  public void handle(Outcome outcome, GameplayScore runScore) {
    if (outcome != Outcome.GAME_WON && outcome != Outcome.GAME_LOST) {
      return;
    }
    if (runScore == null) {
      return;
    }
    double score = calculator.score(runScore);
    // Wins always display 100% progress; losses display the run's actual progress.
    double displayProgress = outcome == Outcome.GAME_WON ? 1.0 : runScore.progressPercent();
    String userId = userIdSupplier.get();
    RecordFlags flags = computeRecordFlags(userId, runScore, score, displayProgress);

    StatsReceiver target = outcome == Outcome.GAME_WON ? winReceiver : loseReceiver;
    if (target != null) {
      target.setStats(runScore.elapsedSeconds(), score, displayProgress,
          flags.newTime(), flags.newScore(), flags.newProgress());
    }

    if (userId != null) {
      repository.save(new PlayHistoryEntry(
          null, userId, runScore.levelId(),
          runScore.distanceTraveled(), runScore.elapsedSeconds(), displayProgress,
          score, runScore.won(), Instant.now()));
    }
  }

  private RecordFlags computeRecordFlags(String userId, GameplayScore runScore,
      double currentScore, double currentProgress) {
    if (userId == null) {
      return RecordFlags.all();
    }
    List<PlayHistoryEntry> priorRunsForLevel = repository.findAllByUser(userId).stream()
        .filter(e -> e.levelId().equals(runScore.levelId()))
        .toList();
    if (priorRunsForLevel.isEmpty()) {
      return RecordFlags.all();
    }
    double bestTime = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::elapsedSeconds).min().orElse(Double.POSITIVE_INFINITY);
    double bestScore = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::score).max().orElse(Double.NEGATIVE_INFINITY);
    double bestProgress = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::progressPercent).max().orElse(Double.NEGATIVE_INFINITY);
    return new RecordFlags(
        runScore.elapsedSeconds() < bestTime,
        currentScore > bestScore,
        currentProgress > bestProgress);
  }

  private record RecordFlags(boolean newTime, boolean newScore, boolean newProgress) {
    static RecordFlags all() {
      return new RecordFlags(true, true, true);
    }
  }
}
