package oogasalad.data.api;

/**
 * Object placed on the map (pickup, hazard, end flag, etc.).
 *
 * @param id                unique key within its game catalog
 * @param spriteUrl         path to the object's sprite asset
 * @param mass              physics mass (≥ 0); 0 = sensor / trigger only
 * @param onCollisionAction engine behavior on collision; null = inert decoration
 */
public record StaticComponentInfo(
    String id,
    String spriteUrl,
    double mass,
    CollisionAction onCollisionAction) {}
