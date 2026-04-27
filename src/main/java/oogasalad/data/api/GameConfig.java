package oogasalad.data.api;

import java.util.List;

/**
 * Read-only view of a parsed game catalog (game.json).
 *
 * <p>Typed accessors reflect the new game schema. {@link #getSetting} /
 * {@link #getNumericSetting} are kept for legacy callers (input mapping, etc.)
 * that still use dot-path look-ups.
 */
public interface GameConfig extends Settings {

  String gameId();

  String mainCharacterId();

  List<ComponentInfo> components();

  List<StaticComponentInfo> staticComponents();

  List<TerrainInfo> terrainTypes();
}
