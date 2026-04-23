package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlitePlayHistoryRepoTest {

  private PlayHistoryRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqlitePlayHistoryRepo("jdbc:sqlite::memory:");
  }

  private PlayHistoryEntry draft(String userId, String levelId, double score, boolean won,
      long finishedAtMs) {
    return new PlayHistoryEntry(
        null, userId, levelId, 1000.0, 30.0, 0.0, score, won,
        Instant.ofEpochMilli(finishedAtMs));
  }

  @Test
  void save_assignsId_andRoundtripsThroughFindAll() {
    PlayHistoryEntry saved = repo.save(draft("u1", "level-1", 80.0, true, 100));

    assertNotNull(saved.id());
    List<PlayHistoryEntry> all = repo.findAllByUser("u1");
    assertEquals(1, all.size());
    assertEquals(saved, all.get(0));
  }

  @Test
  void findBestForLevel_returnsHighestScoreRun() {
    repo.save(draft("u1", "level-1", 50.0, false, 100));
    repo.save(draft("u1", "level-1", 90.0, true, 200));
    repo.save(draft("u1", "level-1", 70.0, true, 300));

    Optional<PlayHistoryEntry> best = repo.findBestForLevel("u1", "level-1");

    assertTrue(best.isPresent());
    assertEquals(90.0, best.get().score());
  }

  @Test
  void findBestForLevel_isScopedByBothUserAndLevel() {
    repo.save(draft("u1", "level-1", 50.0, true, 100));
    repo.save(draft("u2", "level-1", 99.0, true, 200));   // different user
    repo.save(draft("u1", "level-2", 99.0, true, 300));   // different level

    Optional<PlayHistoryEntry> best = repo.findBestForLevel("u1", "level-1");

    assertTrue(best.isPresent());
    assertEquals(50.0, best.get().score());
  }

  @Test
  void findBestForLevel_returnsEmptyWhenNoRunsMatch() {
    Optional<PlayHistoryEntry> best = repo.findBestForLevel("nobody", "level-1");

    assertTrue(best.isEmpty());
  }

  @Test
  void findAllByUser_orderedByFinishedAtDescending() {
    repo.save(draft("u1", "level-1", 50.0, true, 100));
    repo.save(draft("u1", "level-2", 60.0, true, 300));
    repo.save(draft("u1", "level-1", 70.0, true, 200));

    List<PlayHistoryEntry> all = repo.findAllByUser("u1");

    assertEquals(3, all.size());
    assertEquals(300, all.get(0).finishedAt().toEpochMilli());
    assertEquals(200, all.get(1).finishedAt().toEpochMilli());
    assertEquals(100, all.get(2).finishedAt().toEpochMilli());
  }

  @Test
  void findAllByUser_returnsEmptyList_whenUserHasNoRuns() {
    List<PlayHistoryEntry> none = repo.findAllByUser("ghost-user");

    assertTrue(none.isEmpty());
  }

  @Test
  void findAllByUser_excludesRunsBelongingToOtherUsers() {
    repo.save(draft("u1", "level-1", 80.0, true, 100));
    repo.save(draft("u1", "level-2", 90.0, true, 200));

    List<PlayHistoryEntry> othersView = repo.findAllByUser("u2");

    assertTrue(othersView.isEmpty());
  }

  @Test
  void queriesOnFreshlyInitializedRepo_returnEmpty_withoutThrowing() {
    // The constructor runs initSchema(), so the table exists — but no rows have been inserted.
    // Both query methods must still work against the empty table, not NPE or throw SQL errors.
    PlayHistoryRepository fresh = new SqlitePlayHistoryRepo("jdbc:sqlite::memory:");

    assertTrue(fresh.findBestForLevel("anyone", "anywhere").isEmpty());
    assertTrue(fresh.findAllByUser("anyone").isEmpty());
  }

  @Test
  void save_withNullUserId_surfacesErrorAsRuntimeException() {
    // user_id is NOT NULL in the schema; a null must be surfaced, not silently written.
    PlayHistoryEntry badEntry = new PlayHistoryEntry(
        null, null, "level-1", 0.0, 0.0, 0.0, 0.0, false, Instant.EPOCH);

    assertThrows(RuntimeException.class, () -> repo.save(badEntry));
  }
}
