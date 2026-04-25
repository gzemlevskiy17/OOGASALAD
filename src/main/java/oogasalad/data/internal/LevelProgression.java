package oogasalad.data.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import oogasalad.data.api.LevelId;

/**
 * Computes level ordering and unlock state from two inputs:
 *
 * <ol>
 *   <li>A hardcoded canonical order for worlds and for levels-within-a-world. Baked into this
 *       class so progression logic doesn't depend on filesystem ordering or caller-supplied
 *       lists.</li>
 *   <li>A caller-supplied dictionary of which (world → levels) actually exist. Today this is
 *       hardcoded at the call site; later it'll be auto-detected from {@code data/levels/}.
 *       Supplying it separately keeps progression rules stable as level inventory changes.</li>
 * </ol>
 *
 * <p>On construction, the dictionary is filtered and re-sorted against the canonical orders so
 * anything downstream (flat list, {@link #nextAfter}, {@link #isUnlocked}) operates on a known
 * play order. Entries for unknown worlds or unknown levels are silently skipped — preferable
 * to throwing, so that a typo in the dictionary can't crash startup.
 *
 * <p>Unlock rule: first level overall is always unlocked; every other is unlocked iff the run
 * history contains a winning entry for the preceding level. Completion is binary.
 */
public class LevelProgression {

  /** Worlds in canonical play order. Also used by {@code LevelScanner} for filtering. */
  public static final List<String> WORLD_ORDER =
      List.of("forest", "desert", "moon", "glacier", "mars");

  /** Level keys in canonical play order (within a world). Also used by scanner filtering. */
  public static final List<String> LEVEL_ORDER =
      List.of("first", "second", "third", "fourth", "fifth");

  private final Map<String, List<String>> orderedLevelsByWorld;
  private final List<LevelId> flatOrder;

  /**
   * @param availableLevelsByWorld dictionary of which levels exist per world; key and values
   *     outside the canonical orders are ignored. Input order is irrelevant — the canonical
   *     orders drive output.
   */
  public LevelProgression(Map<String, ? extends Collection<String>> availableLevelsByWorld) {
    Map<String, List<String>> ordered = new LinkedHashMap<>();
    List<LevelId> flat = new ArrayList<>();
    for (String world : WORLD_ORDER) {
      Collection<String> available = availableLevelsByWorld.get(world);
      if (available == null || available.isEmpty()) {
        continue;
      }
      List<String> sortedLevels = new ArrayList<>();
      for (String level : LEVEL_ORDER) {
        if (available.contains(level)) {
          sortedLevels.add(level);
          flat.add(new LevelId(world, level));
        }
      }
      if (!sortedLevels.isEmpty()) {
        ordered.put(world, List.copyOf(sortedLevels));
      }
    }
    this.orderedLevelsByWorld = Collections.unmodifiableMap(ordered);
    this.flatOrder = List.copyOf(flat);
  }

  /** World keys that survived the dictionary, sorted by canonical world order. */
  public List<String> worlds() {
    return List.copyOf(orderedLevelsByWorld.keySet());
  }

  /** Level keys in the given world, sorted by canonical level order; empty if unknown. */
  public List<String> levelsIn(String worldKey) {
    return orderedLevelsByWorld.getOrDefault(worldKey, List.of());
  }

  /** Every level in play order, flattened across worlds. */
  public List<LevelId> all() {
    return flatOrder;
  }

  /** Path the level loader reads; constructed from the id, not validated. */
  public String pathFor(LevelId id) {
    return "data/levels/" + id.worldKey() + "/" + id.levelKey() + ".json";
  }

  /** Next level in play order, or empty when {@code current} is the last or unknown. */
  public Optional<LevelId> nextAfter(LevelId current) {
    int index = flatOrder.indexOf(current);
    if (index < 0 || index >= flatOrder.size() - 1) {
      return Optional.empty();
    }
    return Optional.of(flatOrder.get(index + 1));
  }

  /**
   * Cursor-based unlock: a level is unlocked iff its index in the current play order is at most
   * one past the user's {@code furthestReached} cursor. {@code null} cursor (brand-new user) is
   * treated as -1, leaving only the very first level unlocked. A cursor that doesn't appear in
   * the current play order (e.g., a renamed level) is also treated as -1 — defensive.
   *
   * <p>Choosing the cursor over a "predecessor was won" rule means inserting new levels into
   * earlier worlds doesn't re-lock downstream levels the user already cleared.
   */
  public boolean isUnlocked(LevelId id, LevelId furthestReached) {
    int index = flatOrder.indexOf(id);
    if (index < 0) {
      return false;
    }
    int cursorIndex = furthestReached != null ? flatOrder.indexOf(furthestReached) : -1;
    return index <= cursorIndex + 1;
  }
}
