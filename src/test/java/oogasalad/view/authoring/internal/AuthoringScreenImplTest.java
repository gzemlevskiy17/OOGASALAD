package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.GameAttributes;
import oogasalad.data.internal.GameConfigImpl;
import org.junit.jupiter.api.Test;

class AuthoringScreenImplTest {

  private static final GameConfig IURI_CONFIG =
      new GameConfigImpl("data/games/iuriiworld/game.json");

  private static List<String> applyLikeAuthoring(List<String> selectedIds, GameConfig gameConfig) {
    if (selectedIds == null) {
      return List.of();
    }
    if (gameConfig == null) {
      return List.copyOf(selectedIds);
    }
    HashSet<String> movable =
        new HashSet<>(new GameAttributes(gameConfig).movableComponentIds());
    return selectedIds.stream().filter(movable::contains).toList();
  }

  @Test
  void apply_null_selection_yields_empty_list() {
    assertEquals(List.of(), applyLikeAuthoring(null, IURI_CONFIG));
  }

  @Test
  void apply_empty_selection_with_catalog_yields_empty_list() {
    assertEquals(List.of(), applyLikeAuthoring(List.of(), IURI_CONFIG));
  }

  @Test
  void apply_without_catalog_passes_selection_through_unchanged() {
    List<String> pick = List.of("Frame", "Wheel");
    assertEquals(pick, applyLikeAuthoring(pick, null));
  }

  @Test
  void apply_unknown_id_is_dropped_when_wheel_only_catalog() {
    assertEquals(List.of(), applyLikeAuthoring(List.of("Frame"), IURI_CONFIG));
  }

  @Test
  void apply_wheel_id_is_kept_when_in_catalog() {
    assertEquals(List.of("Wheel"), applyLikeAuthoring(List.of("Wheel"), IURI_CONFIG));
  }

  @Test
  void apply_mixed_selection_keeps_wheel_in_original_order_among_valid_ids() {
    assertEquals(List.of("Wheel"), applyLikeAuthoring(List.of("Frame", "Wheel"), IURI_CONFIG));
  }

  @Test
  void apply_wheel_before_unknown_id_still_keeps_only_wheel() {
    assertEquals(List.of("Wheel"), applyLikeAuthoring(List.of("Wheel", "Frame"), IURI_CONFIG));
  }
}