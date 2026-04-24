package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import oogasalad.data.api.LevelId;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteUserProgressionRepoTest {

  private UserProgressionRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqliteUserProgressionRepo("jdbc:sqlite::memory:");
  }

  @Test
  void save_andFindByUser_roundtrip() {
    UserProgression cursor = new UserProgression(
        "alice", new LevelId("forest", "second"), Instant.ofEpochMilli(100));

    repo.save(cursor);
    Optional<UserProgression> loaded = repo.findByUser("alice");

    assertTrue(loaded.isPresent());
    assertEquals(cursor, loaded.get());
  }

  @Test
  void save_upserts_keepingOnlyLatestRowPerUser() {
    repo.save(new UserProgression(
        "alice", new LevelId("forest", "first"), Instant.ofEpochMilli(100)));
    repo.save(new UserProgression(
        "alice", new LevelId("desert", "first"), Instant.ofEpochMilli(200)));

    Optional<UserProgression> latest = repo.findByUser("alice");

    assertTrue(latest.isPresent());
    assertEquals(new LevelId("desert", "first"), latest.get().lastLevel());
    assertEquals(200L, latest.get().updatedAt().toEpochMilli());
  }

  @Test
  void findByUser_returnsEmpty_whenDatabaseHasNoRows() {
    assertTrue(repo.findByUser("nobody").isEmpty());
  }

  @Test
  void twoUsers_haveIndependentCursors() {
    repo.save(new UserProgression(
        "alice", new LevelId("forest", "first"), Instant.ofEpochMilli(100)));
    repo.save(new UserProgression(
        "bob", new LevelId("mars", "first"), Instant.ofEpochMilli(200)));

    assertEquals(new LevelId("forest", "first"), repo.findByUser("alice").get().lastLevel());
    assertEquals(new LevelId("mars", "first"), repo.findByUser("bob").get().lastLevel());
  }

  @Test
  void save_withNullUserId_surfacesErrorAsRuntimeException() {
    UserProgression bad = new UserProgression(
        null, new LevelId("forest", "first"), Instant.EPOCH);

    assertThrows(RuntimeException.class, () -> repo.save(bad));
  }

  @Test
  void findByUser_forExistingUser_returnsTheirStoredCursor() {
    UserProgression saved = new UserProgression(
        "alice", new LevelId("forest", "third"), Instant.ofEpochMilli(500));
    repo.save(saved);

    Optional<UserProgression> found = repo.findByUser("alice");

    assertTrue(found.isPresent());
    assertEquals("alice", found.get().userId());
    assertEquals(new LevelId("forest", "third"), found.get().lastLevel());
    assertEquals(500L, found.get().updatedAt().toEpochMilli());
  }

  @Test
  void findByUser_forUnknownUser_whenOtherUsersExist_returnsEmpty() {
    repo.save(new UserProgression(
        "alice", new LevelId("forest", "first"), Instant.ofEpochMilli(100)));
    repo.save(new UserProgression(
        "bob", new LevelId("desert", "first"), Instant.ofEpochMilli(200)));

    assertTrue(repo.findByUser("charlie").isEmpty());
  }
}
