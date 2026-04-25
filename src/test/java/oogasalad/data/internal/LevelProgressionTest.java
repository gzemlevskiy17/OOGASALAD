package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import oogasalad.data.api.LevelId;
import org.junit.jupiter.api.Test;

class LevelProgressionTest {

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
  void isUnlocked_firstLevelOverall_isUnlockedForBrandNewUser() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first"),
        "desert", List.of("first")));

    assertTrue(p.isUnlocked(new LevelId("forest", "first"), null));
  }

  @Test
  void isUnlocked_secondLevel_isLockedForBrandNewUser() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second")));

    assertFalse(p.isUnlocked(new LevelId("forest", "second"), null));
  }

  @Test
  void isUnlocked_oneLevelPastCursor_isUnlocked() {
    // Cursor at forest/first → forest/second (one past) is unlocked.
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second")));

    assertTrue(p.isUnlocked(new LevelId("forest", "second"), new LevelId("forest", "first")));
  }

  @Test
  void isUnlocked_twoLevelsPastCursor_isLocked() {
    // Cursor at forest/first → forest/third is two past, not unlocked.
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second", "third")));

    assertFalse(p.isUnlocked(new LevelId("forest", "third"), new LevelId("forest", "first")));
  }

  @Test
  void isUnlocked_levelAtOrBeforeCursor_isUnlocked() {
    // Cursor at desert/first; everything from forest/first up through desert/first stays open.
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second"),
        "desert", List.of("first")));
    LevelId cursor = new LevelId("desert", "first");

    assertTrue(p.isUnlocked(new LevelId("forest", "first"), cursor));
    assertTrue(p.isUnlocked(new LevelId("forest", "second"), cursor));
    assertTrue(p.isUnlocked(new LevelId("desert", "first"), cursor));
  }

  @Test
  void isUnlocked_cursorAtLastLevel_unlocksEverything() {
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second"),
        "desert", List.of("first")));
    LevelId cursor = new LevelId("desert", "first");

    for (LevelId id : p.all()) {
      assertTrue(p.isUnlocked(id, cursor));
    }
  }

  @Test
  void isUnlocked_unknownLevel_isFalse_regardlessOfCursor() {
    LevelProgression p = new LevelProgression(Map.of("forest", List.of("first")));

    assertFalse(p.isUnlocked(new LevelId("atlantis", "first"), null));
    assertFalse(p.isUnlocked(new LevelId("atlantis", "first"), new LevelId("forest", "first")));
  }

  @Test
  void isUnlocked_unknownCursor_treatedAsNoProgress() {
    // A cursor pointing at a level that no longer exists in the play order shouldn't accidentally
    // unlock everything — fall back to "brand new user" behavior (only level 0 unlocked).
    LevelProgression p = new LevelProgression(Map.of(
        "forest", List.of("first", "second")));
    LevelId staleCursor = new LevelId("atlantis", "first");

    assertTrue(p.isUnlocked(new LevelId("forest", "first"), staleCursor));
    assertFalse(p.isUnlocked(new LevelId("forest", "second"), staleCursor));
  }

  @Test
  void isUnlocked_addingNewLevelsMidWorld_doesNotRegressDownstreamUnlocks() {
    // Original layout: forest/first → desert/first → moon/first.
    // User cleared up through moon/first, cursor stored as (moon, first).
    // Then forest gets second/third dropped in. Cursor at moon/first now sits at index 4 of
    // the new flat order. Everything up to glacier/first (one past cursor) should be unlocked,
    // including the freshly-inserted forest/second and forest/third — and crucially desert/first
    // shouldn't get re-locked just because a new predecessor was inserted.
    LevelProgression after = new LevelProgression(Map.of(
        "forest", List.of("first", "second", "third"),
        "desert", List.of("first"),
        "moon", List.of("first"),
        "glacier", List.of("first"),
        "mars", List.of("first")));
    LevelId cursor = new LevelId("moon", "first");

    assertTrue(after.isUnlocked(new LevelId("forest", "second"), cursor));
    assertTrue(after.isUnlocked(new LevelId("forest", "third"), cursor));
    assertTrue(after.isUnlocked(new LevelId("desert", "first"), cursor));    // no regression
    assertTrue(after.isUnlocked(new LevelId("glacier", "first"), cursor));   // one past cursor
    assertFalse(after.isUnlocked(new LevelId("mars", "first"), cursor));     // two past cursor
  }

  @Test
  void emptyDictionary_yieldsEmptyEverything() {
    LevelProgression p = new LevelProgression(Map.of());

    assertTrue(p.worlds().isEmpty());
    assertTrue(p.all().isEmpty());
    assertTrue(p.nextAfter(new LevelId("forest", "first")).isEmpty());
    assertFalse(p.isUnlocked(new LevelId("forest", "first"), null));
  }
}
