package oogasalad.model.player.api;

import java.util.List;
import java.util.Optional;
import oogasalad.data.api.LevelId;

/** Per-user cursor storage. */
public interface UserProgressionRepository {

  /** Returns the user's saved progression if one exists. */
  Optional<UserProgression> findByUser(String userId);

  /**
   * Upserts {@code lastClicked} for the user. Preserves the existing {@code furthestReached}
   * if a row is present; leaves it null on first insert.
   */
  void saveLastClicked(String userId, LevelId clicked);

  /**
   * Upserts {@code furthestReached} only if {@code newLevel}'s index in {@code playOrder} is
   * greater than the current stored level's index. Preserves {@code lastClicked} (or defaults
   * to {@code newLevel} on first insert). Called from the win path after a level is completed.
   */
  void advanceFurthestReached(String userId, LevelId newLevel, List<LevelId> playOrder);
}
