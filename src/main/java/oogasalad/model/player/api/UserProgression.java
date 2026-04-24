package oogasalad.model.player.api;

import java.time.Instant;
import oogasalad.data.api.LevelId;

/**
 * Persistent per-user cursor recording the last level the user started. One row per user.
 * Updated every time the user picks a level to play. Used to pre-select the entry screen on
 * next launch and, later, to power a "Continue" button.
 */
public record UserProgression(
    String userId,
    LevelId lastLevel,
    Instant updatedAt
) {}
