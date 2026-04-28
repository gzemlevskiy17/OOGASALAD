package oogasalad.data.api;

/**
 * Terrain material available for a game's spline tracks.
 *
 * @param id          catalog id referenced from level files
 * @param spriteUrl   tile texture path; may be null
 * @param visualColor solid-color fallback when no sprite is provided; may be null
 * @param trackColor  CSS color used for the spline track line; may be null
 * @param friction    surface friction coefficient (>= 0)
 * @author Paulina Vvedenskaya
 */
public record TerrainInfo(String id, String spriteUrl, String visualColor, String trackColor,
                          double friction) {

  public TerrainInfo(String id, String spriteUrl, String visualColor, double friction) {
    this(id, spriteUrl, visualColor, null, friction);
  }
}
