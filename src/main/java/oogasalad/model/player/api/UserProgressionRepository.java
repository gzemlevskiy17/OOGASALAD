package oogasalad.model.player.api;

import java.util.Optional;

/** Per-user cursor storage. Upsert semantics: one row per user, overwritten on each save. */
public interface UserProgressionRepository {

  /** Upserts the user's progression cursor. Overwrites any prior row for the same user. */
  void save(UserProgression progression);

  /** Returns the user's saved cursor if one exists. */
  Optional<UserProgression> findByUser(String userId);
}
