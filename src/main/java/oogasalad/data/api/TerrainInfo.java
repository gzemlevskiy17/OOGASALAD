package oogasalad.data.api;

/**
 * Catalog entry for a terrain material. {@link #spriteUrl} is the tile texture; if null, the
 * renderer falls back to {@link #visualColor}.
 *
 * @param id          catalog id referenced from level files
 * @param spriteUrl   tile texture path; may be null
 * @param visualColor solid-color fallback when no sprite is provided; may be null
 * @param friction    surface friction
 * @author Paulina Vvedenskaya
 */
public record TerrainInfo(
    String id,
    String spriteUrl,
    String visualColor,
    double friction
) {

}
