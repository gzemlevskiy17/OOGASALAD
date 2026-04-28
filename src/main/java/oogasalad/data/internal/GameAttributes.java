package oogasalad.data.internal;

import java.util.List;
import java.util.Objects;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;

/**
 * Typed view over a game's catalog ({@code game.json}). This class only reflects game data — it does
 * not read level files. Callers combine it with {@link LevelAttributes} (e.g. {@code elementsAllowed})
 * where level-specific overrides matter.
 *
 * @author Iurii Beliaev
 */
public final class GameAttributes {

  private final GameConfig game;

  public GameAttributes(GameConfig game) {
    this.game = Objects.requireNonNull(game, "game");
  }

  public String gameId() {
    return game.gameId();
  }

  /** Returns MainCharacterComponentId */
  public String mainCharacterComponentId() {
    return game.mainCharacterId();
  }

  /** Returns moveableComponentIds */
  public List<String> movableComponentIds() {
    return game.components().stream()
        .map(ComponentInfo::id)
        .filter(id -> id != null && !id.isBlank())
        .toList();
  }

  /** Returns staticComponentIds */
  public List<String> staticComponentIds() {
    return game.staticComponents().stream()
        .map(StaticComponentInfo::id)
        .filter(id -> id != null && !id.isBlank())
        .toList();
  }

  /** Every {@link TerrainInfo#id} under {@code terrainTypes}. */
  public List<String> terrainTypeIds() {
    return game.terrainTypes().stream()
        .map(TerrainInfo::id)
        .filter(id -> id != null && !id.isBlank())
        .toList();
  }

  /** Underlying config for callers that need full typed access. */
  public GameConfig gameConfig() {
    return game;
  }
}