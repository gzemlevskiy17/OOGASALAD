package oogasalad.model.player.api;

import java.time.Instant;
import oogasalad.data.api.LevelId;

/**
 * Per-user progression state. Two cursors with different update rules:
 *
 * <ul>
 *   <li>{@code furthestReached}: the deepest level the user has won (by play-order index).
 *       Advances only via {@code advanceFurthestReached} on the repo, and only when the new
 *       level beats the current one. May be {@code null} if the user has never won a level.</li>
 *   <li>{@code lastClicked}: the level they most recently selected to play. Overwritten on
 *       every selection. Always non-null once a row exists.</li>
 * </ul>
 */
public record UserProgression(
    String userId,
    LevelId furthestReached,
    LevelId lastClicked,
    Instant updatedAt
) {}
