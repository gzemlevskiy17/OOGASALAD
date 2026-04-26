package oogasalad.data.api;

/**
 * Terrain material available for a game's spline tracks.
 *
 * @param id           unique key within its game catalog
 * @param spriteUrl    tile texture; null falls back to {@code visualColor}
 * @param visualColor  solid-color fallback when no sprite is provided; null = engine default
 * @param friction     surface friction coefficient (≥ 0)
 */
public record TerrainInfo(String id, String spriteUrl, String visualColor, double friction) {}
