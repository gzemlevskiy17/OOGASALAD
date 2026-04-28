package oogasalad.controller.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.GameAttributes;
import oogasalad.data.internal.GameConfigImpl;
import org.junit.jupiter.api.Test;

class ScreenFactoryTest {

  private static List<String> resolvedVehiclePaletteIds(GameConfig gameConfig) {
    return new GameAttributes(gameConfig).movableComponentIds();
  }

  @Test
  void resolved_palette_for_iurii_world_has_only_wheel() {
    GameConfig cfg = new GameConfigImpl("data/games/iuriiworld/game.json");
    assertEquals(List.of("Wheel"), resolvedVehiclePaletteIds(cfg));
  }

  @Test
  void resolved_palette_with_null_game_config_throws_like_game_attributes() {
    assertThrows(NullPointerException.class, () -> resolvedVehiclePaletteIds(null));
  }

  @Test
  void resolved_palette_with_empty_components_list_is_empty() {
    GameConfig cfg = GameConfigImpl.empty();
    assertEquals(List.of(), resolvedVehiclePaletteIds(cfg));
  }
}