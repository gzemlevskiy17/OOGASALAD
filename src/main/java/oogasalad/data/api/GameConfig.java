package oogasalad.data.api;

import java.util.List;

/**
 * In-memory shape of a game.json file: the catalog of types shared by every level of one game.
 * Mirrors the JSON schema one-to-one and is loaded once per game.
 *
 * @param gameId             unique identifier referenced by levels via gameMetadata.gameType
 * @param primaryComponentId id (from {@code components}) of the chassis component the camera tracks
 *                           and that owns HP at runtime
 * @param components         movable parts the player can use in the vehicle builder
 * @param staticComponents   placed-on-the-map objects (pickups, hazards, end flag, etc.)
 * @param terrainTypes       terrain materials available for tracks in any level of this game
 * @author Paulina Vvedenskaya
 */
public record GameConfig(
    String gameId,
    String primaryComponentId,
    List<ComponentInfo> components,
    List<StaticComponentInfo> staticComponents,
    List<TerrainInfo> terrainTypes
) {
}
