package oogasalad.view.authoring.internal.level;

/**
 * Live snapshot of a terrain type from the authoring catalog ({@code TerrainBlock.properties} +
 * palette) for the properties sidebar.
 */
public record CatalogTerrainInfo(
    String catalogName, double friction, double restitution, String sprite) {
}
