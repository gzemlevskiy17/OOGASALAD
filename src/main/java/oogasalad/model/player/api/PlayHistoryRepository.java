package oogasalad.model.player.api;

import java.util.List;
import java.util.Optional;

/** Persists per-playthrough records keyed by user. Append-only; rows are never updated. */
public interface PlayHistoryRepository {

  /** Persists the entry and returns it with the repo-assigned id populated. */
  PlayHistoryEntry save(PlayHistoryEntry entry);

  /** Returns the user's highest-score run on the given level, if any exists. */
  Optional<PlayHistoryEntry> findBestForLevel(String userId, String levelId);

  /** Returns all of the user's runs, most recent {@code finishedAt} first. */
  List<PlayHistoryEntry> findAllByUser(String userId);
}
