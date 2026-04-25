package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import oogasalad.data.api.LevelId;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteUserProgressionRepoTest {

  private static final List<LevelId> PLAY_ORDER = List.of(
      new LevelId("forest", "first"),
      new LevelId("forest", "second"),
      new LevelId("forest", "third"),
      new LevelId("desert", "first"));

  private UserProgressionRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqliteUserProgressionRepo("jdbc:sqlite::memory:");
  }

  @Test
  void findByUser_returnsEmpty_whenDatabaseHasNoRows() {
    assertTrue(repo.findByUser("nobody").isEmpty());
  }

  @Test
  void saveLastClicked_insertsRowWithNullFurthest_forFirstTimeUser() {
    repo.saveLastClicked("alice", new LevelId("forest", "first"));

    Optional<UserProgression> loaded = repo.findByUser("alice");
    assertTrue(loaded.isPresent());
    assertNull(loaded.get().furthestReached());
    assertEquals(new LevelId("forest", "first"), loaded.get().lastClicked());
  }

  @Test
  void saveLastClicked_preservesFurthest_onExistingRow() {
    repo.advanceFurthestReached("alice", new LevelId("forest", "second"), PLAY_ORDER);
    repo.saveLastClicked("alice", new LevelId("forest", "first"));

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertEquals(new LevelId("forest", "second"), loaded.furthestReached());
    assertEquals(new LevelId("forest", "first"), loaded.lastClicked());
  }

  @Test
  void advanceFurthestReached_insertsRow_forFirstTimeUser() {
    repo.advanceFurthestReached("alice", new LevelId("forest", "first"), PLAY_ORDER);

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertEquals(new LevelId("forest", "first"), loaded.furthestReached());
    // lastClicked defaults to the advanced level on first-row creation.
    assertEquals(new LevelId("forest", "first"), loaded.lastClicked());
  }

  @Test
  void advanceFurthestReached_advancesToGreaterLevel_keepingLastClicked() {
    repo.saveLastClicked("alice", new LevelId("forest", "first"));
    repo.advanceFurthestReached("alice", new LevelId("forest", "second"), PLAY_ORDER);

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertEquals(new LevelId("forest", "second"), loaded.furthestReached());
    assertEquals(new LevelId("forest", "first"), loaded.lastClicked());
  }

  @Test
  void advanceFurthestReached_ignoresEarlierLevel() {
    repo.advanceFurthestReached("alice", new LevelId("forest", "third"), PLAY_ORDER);
    repo.advanceFurthestReached("alice", new LevelId("forest", "first"), PLAY_ORDER);

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertEquals(new LevelId("forest", "third"), loaded.furthestReached());
  }

  @Test
  void advanceFurthestReached_ignoresEqualLevel() {
    repo.advanceFurthestReached("alice", new LevelId("forest", "second"), PLAY_ORDER);
    repo.advanceFurthestReached("alice", new LevelId("forest", "second"), PLAY_ORDER);

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertEquals(new LevelId("forest", "second"), loaded.furthestReached());
  }

  @Test
  void advanceFurthestReached_ignoresLevelNotInPlayOrder() {
    repo.saveLastClicked("alice", new LevelId("forest", "first"));
    repo.advanceFurthestReached("alice", new LevelId("atlantis", "first"), PLAY_ORDER);

    UserProgression loaded = repo.findByUser("alice").orElseThrow();
    assertNull(loaded.furthestReached());
  }

  @Test
  void twoUsers_haveIndependentProgression() {
    repo.advanceFurthestReached("alice", new LevelId("forest", "second"), PLAY_ORDER);
    repo.advanceFurthestReached("bob", new LevelId("desert", "first"), PLAY_ORDER);

    assertEquals(new LevelId("forest", "second"),
        repo.findByUser("alice").orElseThrow().furthestReached());
    assertEquals(new LevelId("desert", "first"),
        repo.findByUser("bob").orElseThrow().furthestReached());
  }

  @Test
  void findByUser_forUnknownUser_whenOtherUsersExist_returnsEmpty() {
    repo.saveLastClicked("alice", new LevelId("forest", "first"));
    repo.saveLastClicked("bob", new LevelId("desert", "first"));

    assertTrue(repo.findByUser("charlie").isEmpty());
  }
}
