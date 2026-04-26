package oogasalad.data.api;

/**
 * Catalog entry for a static, placed-on-the-map object (pickup, hazard, finish line, etc.).
 *
 * @param id                catalog id referenced by mapObject.type in level files
 * @param spriteUrl         path to the object's sprite
 * @param mass              physics mass (often unused for triggers)
 * @param onCollisionAction behavior fired on contact, or null if the object is inert
 * @author Paulina Vvedenskaya
 */
public record StaticComponentInfo(
    String id,
    String spriteUrl,
    double mass,
    OnCollisionAction onCollisionAction
) {

}
