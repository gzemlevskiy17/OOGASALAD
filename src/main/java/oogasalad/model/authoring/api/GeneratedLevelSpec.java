package oogasalad.model.authoring.api;

import java.util.List;

/**
 * Schema-aligned intermediate specification produced by AI level generation before validation,
 * normalization, and serialization into the repository's level-file format.
 *
 * <p>The canonical shape mirrors the new level schema:
 * <ul>
 *   <li>{@code gameMetadata}</li>
 *   <li>{@code map}</li>
 *   <li>{@code mainCharacter}</li>
 * </ul>
 *
 * @param gameMetadata level-wide runtime metadata
 * @param map authored map data including knots, spawn point, and placed objects
 * @param mainCharacter player/build-constraint metadata for the level
 * @author Emilio Garcia
 */
public record GeneratedLevelSpec(
    GameMetadataSpec gameMetadata,
    MapSpec map,
    MainCharacterSpec mainCharacter) {

  /**
   * Creates an immutable schema-aligned generated-level specification.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelSpec {
    gameMetadata = gameMetadata == null ? new GameMetadataSpec(null, null, null, null, null, null)
        : gameMetadata;
    map = map == null ? new MapSpec(List.of(), null, List.of(), List.of()) : map;
    mainCharacter = mainCharacter == null
        ? new MainCharacterSpec(null, List.of(), null, null, null)
        : mainCharacter;
  }

  /**
   * Level-wide metadata matching the {@code gameMetadata} schema section.
   *
   * @param levelName authored level name
   * @param gameType referenced game id
   * @param screenWidth logical authored level width
   * @param screenHeight logical authored level height
   * @param gravity world gravity
   * @param drag world drag coefficient
   * @author Emilio Garcia
   */
  public record GameMetadataSpec(
      String levelName,
      String gameType,
      Double screenWidth,
      Double screenHeight,
      Double gravity,
      Double drag) {
  }

  /**
   * Authored map data matching the {@code map} schema section.
   *
   * @param knotLocations spline knot locations for the terrain track
   * @param spawnPoint level spawn point
   * @param objects placed map objects constrained to the selected game catalog
   * @param terrainOptions optional authoring terrain hints
   * @author Emilio Garcia
   */
  public record MapSpec(
      List<PointSpec> knotLocations,
      PointSpec spawnPoint,
      List<MapObjectSpec> objects,
      List<String> terrainOptions) {

    /**
     * Creates immutable map data.
     *
     * @author Emilio Garcia
     */
    public MapSpec {
      knotLocations = knotLocations == null ? List.of() : List.copyOf(knotLocations);
      objects = objects == null ? List.of() : List.copyOf(objects);
      terrainOptions = terrainOptions == null ? List.of() : List.copyOf(terrainOptions);
    }
  }

  /**
   * Main-character metadata matching the {@code mainCharacter} schema section.
   *
   * <p>{@code cameraFollows} is kept as an optional field because the sample Good Piggies level
   * files and level-attribute model already use it, even though the schema file on this branch has
   * not fully caught up yet.
   *
   * @param gridSize vehicle-builder grid size
   * @param elementsAllowed component ids allowed in the builder for this level
   * @param maxSelectable maximum number of buildable components allowed
   * @param maxHP starting/max HP for the tracked main character
   * @param cameraFollows optional camera-follow target convention
   * @author Emilio Garcia
   */
  public record MainCharacterSpec(
      Double gridSize,
      List<String> elementsAllowed,
      Integer maxSelectable,
      Double maxHP,
      String cameraFollows) {

    /**
     * Creates immutable main-character metadata.
     *
     * @author Emilio Garcia
     */
    public MainCharacterSpec {
      elementsAllowed = elementsAllowed == null ? List.of() : List.copyOf(elementsAllowed);
    }
  }

  /**
   * Map object entry matching the schema's placed object structure.
   *
   * @param type referenced static-object or terrain-option id
   * @param position object position
   * @author Emilio Garcia
   */
  public record MapObjectSpec(String type, PointSpec position) {
  }

  /**
   * Point in authored world coordinates.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @author Emilio Garcia
   */
  public record PointSpec(Double x, Double y) {
  }

}
