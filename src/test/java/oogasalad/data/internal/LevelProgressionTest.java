package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import oogasalad.data.api.LevelId;
import oogasalad.model.player.api.PlayHistoryEntry;
import org.junit.jupiter.api.Test;

class LevelProgressionTest {

  private PlayHistoryEntry run(String levelPath, boolean won) {
    return new PlayHistoryEntry(
        "id-" + levelPath + "-" + won, "alice", levelPath,
        1000.0, 30.0, 1.0, 90.0, won, Instant.EPOCH);
  }

  @Test
  void worlds_returnedInCanonicalOrder_regardlessOfInput() {
    LevelProgression p = new LevelProgression(Map.of(
        "moon", List.of("first"),
        "forest", List.of("first"),
        "desert", List.of("first")));

    assertEquals(List.of("forest", "desert", "moon"), p.worlds());
  }

  @Test
  void levelsIn_returnedInCanonicalOrder_regardlessOfInput() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("third", "first", "second")));

    assertEquals(List.of("first", "second", "third"), p.levelsIn("forest"));
  }

  @Test
  void all_flattensWorldsAndLevelsInCanonicalOrder() {
    LevelProgression p = new LevelProgression(Map.of(
        "desert", List.of("first"),
        "forest", List.of("first", "second")));

    assertEquals(List.of(
        new LevelId("forest", "first"),
        new LevelId("forest", "second"),
        new LevelId("desert", "first")
    ), p.all());
  }

  @Test
  void differentWorlds_canHaveDifferentLevelCounts() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second", "third"),
        "desert", List.of("first"),
        "moon", List.of("first", "second")));

    assertEquals(3, p.levelsIn("forest").size());
    assertEquals(1, p.levelsIn("desert").size());
    assertEquals(2, p.levelsIn("moon").size());
    assertEquals(6, p.all().size());
  }

  @Test
  void unknownWorldKey_isSilentlyDropped() {
    LevelProgression p = new LevelProgression(Map.of(
        "atlantis", List.of("first"),
        "forest", List.of("first")));

    assertEquals(List.of("forest"), p.worlds());
  }

  @Test
  void unknownLevelKey_isSilentlyDropped() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("zeroth", "first", "fictional")));

    assertEquals(List.of("first"), p.levelsIn("forest"));
  }

  @Test
  void worldWithEmptyLevelList_isOmittedFromWorlds() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first"),
        "desert", Set.of()));

    assertEquals(List.of("forest"), p.worlds());
  }

  @Test
  void pathFor_usesDataLevelsWorldLevelJson() {
    LevelProgression p = new LevelProgression(Map.of("forest", List.of("first")));

    assertEquals("data/levels/forest/first.json",
        p.pathFor(new LevelId("forest", "first")));
  }

  @Test
  void nextAfter_withinWorld_yieldsNextLevel() {
    LevelProgression p = new LevelProgression(Map.of("forest", List.of("first", "second")));

    assertEquals(Optional.of(new LevelId("forest", "second")),
        p.nextAfter(new LevelId("forest", "first")));
  }

  @Test
  void nextAfter_lastLevelInWorld_rollsToFirstOfNextWorld() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second"),
        "desert", List.of("first")));

    assertEquals(Optional.of(new LevelId("desert", "first")),
        p.nextAfter(new LevelId("forest", "second")));
  }

  @Test
  void nextAfter_veryLastLevel_isEmpty() {
    LevelProgression p = new LevelProgression(Map.of("forest", List.of("first")));

    assertTrue(p.nextAfter(new LevelId("forest", "first")).isEmpty());
  }

  @Test
  void isUnlocked_firstLevelOverall_isAlwaysUnlocked() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first"),
        "desert", List.of("first")));

    assertTrue(p.isUnlocked(new LevelId("forest", "first"), List.of()));
  }

  @Test
  void isUnlocked_secondLevel_unlocksAfterWinningFirst() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second")));
    List<PlayHistoryEntry> history = List.of(run("data/levels/forest/first.json", true));

    assertTrue(p.isUnlocked(new LevelId("forest", "second"), history));
  }

  @Test
  void isUnlocked_secondLevel_staysLockedWithoutWin() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second")));

    assertFalse(p.isUnlocked(new LevelId("forest", "second"), List.of()));
    assertFalse(p.isUnlocked(new LevelId("forest", "second"),
        List.of(run("data/levels/forest/first.json", false))));
  }

  @Test
  void isUnlocked_firstOfNextWorld_unlocksAfterWinningLastOfPrevWorld() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second"),
        "desert", List.of("first")));
    List<PlayHistoryEntry> history = List.of(run("data/levels/forest/second.json", true));

    assertTrue(p.isUnlocked(new LevelId("desert", "first"), history));
  }

  @Test
  void isUnlocked_unknownLevel_isFalse() {
    LevelProgression p = new LevelProgression(Map.of("forest", List.of("first")));

    assertFalse(p.isUnlocked(new LevelId("atlantis", "first"), List.of()));
  }

  @Test
  void emptyDictionary_yieldsEmptyEverything() {
    LevelProgression p = new LevelProgression(Map.of());

    assertTrue(p.worlds().isEmpty());
    assertTrue(p.all().isEmpty());
    assertTrue(p.nextAfter(new LevelId("forest", "first")).isEmpty());
    assertFalse(p.isUnlocked(new LevelId("forest", "first"), List.of()));
  }
}
