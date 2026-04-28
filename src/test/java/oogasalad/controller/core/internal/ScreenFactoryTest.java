package oogasalad.controller.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.internal.GameConfigImpl;
import org.junit.jupiter.api.Test;

class ScreenFactoryTest {

  @Test
  void resolved_palette_for_iurii_world_has_only_wheel() {
    GameConfig cfg = new GameConfigImpl("data/games/iuriiworld/game.json");
    assertEquals(List.of("Wheel"), ScreenFactory.resolvedVehiclePaletteIds(cfg,
        new LevelAttributes()));
  }

  @Test
  void resolved_palette_with_null_game_config_throws_like_game_attributes() {
    assertThrows(NullPointerException.class,
        () -> ScreenFactory.resolvedVehiclePaletteIds(null, new LevelAttributes()));
  }

  @Test
  void resolved_palette_with_empty_components_list_is_empty() {
    GameConfig cfg = GameConfigImpl.empty();
    assertEquals(List.of(), ScreenFactory.resolvedVehiclePaletteIds(cfg, new LevelAttributes()));
  }

  @Test
  void resolved_palette_with_level_allowed_components_uses_level_subset() {
    GameConfig cfg = new GameConfigImpl("data/games/badpiggies/game.json");
    LevelAttributes attributes = new LevelAttributes();
    attributes.setMainCharacterElementsAllowed(List.of("Wood Wheel", "BigPig"));

    assertEquals(List.of("Wood Wheel", "BigPig"),
        ScreenFactory.resolvedVehiclePaletteIds(cfg, attributes));
  }

  @Test
  void resolved_palette_with_empty_level_allowed_components_uses_all_catalog_components() {
    GameConfig cfg = new GameConfigImpl("data/games/badpiggies/game.json");

    assertEquals(List.of("BigPig", "SmallPig", "Balloon", "Fan", "Engine", "Frame", "Sled",
            "Wood Wheel", "Metal Wheel"),
        ScreenFactory.resolvedVehiclePaletteIds(cfg, new LevelAttributes()));
  }
}
