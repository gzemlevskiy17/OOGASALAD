package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.junit.jupiter.api.Test;

class GameAttributesTest {

  private static final String IURI_GAME_JSON = "data/games/iuriiworld/game.json";

  private static GameAttributes iuriGameAttributes() {
    GameConfig cfg = new GameConfigImpl(IURI_GAME_JSON);
    return new GameAttributes(cfg);
  }

  @Test
  void constructor_throws_when_game_is_null() {
    assertThrows(NullPointerException.class, () -> new GameAttributes(null));
  }

  @Test
  void iuri_game_id_is_loaded() {
    assertEquals("iuriiworld", iuriGameAttributes().gameId());
  }

  @Test
  void iuri_main_character_id_is_wheel() {
    assertEquals("Wheel", iuriGameAttributes().mainCharacterComponentId());
  }

  @Test
  void iuri_movable_component_ids_contains_only_wheel() {
    assertEquals(List.of("Wheel"), iuriGameAttributes().movableComponentIds());
  }

  @Test
  void iuri_static_component_ids_is_empty() {
    assertEquals(List.of(), iuriGameAttributes().staticComponentIds());
  }

  @Test
  void iuri_terrain_type_ids_is_empty() {
    assertEquals(List.of(), iuriGameAttributes().terrainTypeIds());
  }

  @Test
  void movable_component_ids_excludes_blank_string_ids_from_components() {
    GameConfig cfg = mock(GameConfig.class);
    when(cfg.components()).thenReturn(
        List.of(
            new ComponentInfo("ok", "", 1.0, List.of()),
            new ComponentInfo(" ", "", 1.0, List.of())));
    GameAttributes ga = new GameAttributes(cfg);
    assertEquals(List.of("ok"), ga.movableComponentIds());
  }

  @Test
  void static_component_ids_maps_ids_from_static_catalog() {
    GameConfig cfg = mock(GameConfig.class);
    when(cfg.components()).thenReturn(List.of());
    when(cfg.staticComponents())
        .thenReturn(List.of(new StaticComponentInfo("Coin", "", 0.0, null)));
    GameAttributes ga = new GameAttributes(cfg);
    assertEquals(List.of("Coin"), ga.staticComponentIds());
  }

  @Test
  void terrain_type_ids_maps_ids_from_terrain_catalog() {
    GameConfig cfg = mock(GameConfig.class);
    when(cfg.components()).thenReturn(List.of());
    when(cfg.staticComponents()).thenReturn(List.of());
    when(cfg.terrainTypes()).thenReturn(List.of(new TerrainInfo("grass", null, "#ffffff", 0.6)));
    GameAttributes ga = new GameAttributes(cfg);
    assertEquals(List.of("grass"), ga.terrainTypeIds());
  }
}