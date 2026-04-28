package oogasalad.model.player.api;

import java.time.Instant;

/** Immutable snapshot of one gameplay run. */
public record GameplayScore(
    String levelId,
    double distanceTraveled,
    double remainingDistance,
    double levelSpan,
    double progressPercent,
    double elapsedSeconds,
    boolean won,
    Instant finishedAt
) {}
