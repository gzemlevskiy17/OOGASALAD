package oogasalad.data.api;

import java.util.List;

/**
 * Read-only view of a parsed game catalog (game.json).
 *
 * <p>Typed accessors reflect the new game schema. {@link #getSetting} /
 * {@link #getNumericSetting} are kept for legacy callers (input mapping, etc.)
 * that still use dot-path look-ups.
 */
public interface GameConfig {

  /** Stable identifier for this game, e.g. {@code "badpiggies"}. */
  String gameId();

  /**
   * Id of the component treated as the chassis: camera-follow target and HP owner.
   * Must match an entry in {@link #components()}.
   */
  String primaryComponentId();

  /** Movable parts the player may use in the vehicle builder. */
  List<ComponentInfo> components();

  /** Map objects (pickups, hazards, end flag, etc.) available for this game. */
  List<StaticComponentInfo> staticComponents();

  /** Terrain materials available for this game's spline tracks. */
  List<TerrainInfo> terrainTypes();

  // ── Legacy dot-path API (kept for GameControllerImpl input-mapping lookups) ──

  /**
   * Retrieves a setting value as a String using a dot-separated path.
   *
   * @param key dot-separated path into the JSON tree, e.g. {@code "input_mapping.activate_engine"}
   * @return value at that path, or {@code null} if missing
   */
  String getSetting(String key);

  /**
   * Retrieves a numeric setting using a dot-separated path.
   *
   * @param key dot-separated path
   * @return double value at that path
   * @throws IllegalArgumentException if the path does not resolve to a number
   */
  double getNumericSetting(String key);
}
