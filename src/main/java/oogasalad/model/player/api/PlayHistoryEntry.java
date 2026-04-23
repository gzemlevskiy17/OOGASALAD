package oogasalad.model.player.api;

import java.time.Instant;

/**
 * One persisted gameplay playthrough for a user. The {@code id} is null on input to
 * {@link PlayHistoryRepository#save} and populated by the repo on the returned value.
 */
public record PlayHistoryEntry(
    String id,
    String userId,
    String levelId,
    double distanceTraveled,
    double elapsedSeconds,
    double progressPercent,
    double score,
    boolean won,
    Instant finishedAt
) {}
