package oogasalad.model.authoring.api;

import java.util.List;

/**
 * Immutable intermediate specification produced by AI level generation before normalization and
 * conversion into the repository's runtime level JSON shape.
 *
 * <p>This model intentionally stays smaller and more stable than the final saved file format so AI
 * generation can target one contract while the downstream JSON builder adapts to loader changes.
 *
 * @param style generation style hint such as blocks, splines, or mixed
 * @param levelName generated level display name
 * @param gravity optional gravity suggestion for {@code LevelAttributes}
 * @param backgroundImage optional background image path for {@code LevelAttributes}
 * @param worldWidth optional world width hint used when synthesizing bounds and finish placement
 * @param spawn optional spawn placement
 * @param finishLine optional finish line placement
 * @param vehiclePalette optional allowed vehicle-component metadata
 * @param terrainBlocks generated block terrain suggestions
 * @param terrainSplines generated spline terrain suggestions
 * @param rocks generated rock obstacle suggestions
 * @param pickups generated pickup suggestions
 * @author Emilio Garcia
 */
public record GeneratedLevelSpec(
    String style,
    String levelName,
    Double gravity,
    String backgroundImage,
    Double worldWidth,
    SpawnSpec spawn,
    FinishLineSpec finishLine,
    VehiclePaletteSpec vehiclePalette,
    List<TerrainBlockSpec> terrainBlocks,
    List<TerrainSplineSpec> terrainSplines,
    List<RockSpec> rocks,
    List<PickupSpec> pickups) {

  /**
   * Creates an immutable generated-level specification.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelSpec {
    terrainBlocks = terrainBlocks == null ? List.of() : List.copyOf(terrainBlocks);
    terrainSplines = terrainSplines == null ? List.of() : List.copyOf(terrainSplines);
    rocks = rocks == null ? List.of() : List.copyOf(rocks);
    pickups = pickups == null ? List.of() : List.copyOf(pickups);
  }

  /**
   * Spawn position suggestion in authored world coordinates.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @author Emilio Garcia
   */
  public record SpawnSpec(Double x, Double y) {
  }

  /**
   * Finish-line placement suggestion in authored world coordinates.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width finish-line width
   * @param height finish-line height
   * @author Emilio Garcia
   */
  public record FinishLineSpec(Double x, Double y, Double width, Double height) {
  }

  /**
   * Allowed vehicle-component metadata for the generated level.
   *
   * @param maxSelectable maximum number of selectable components
   * @param allowedComponents stable component IDs allowed in the level
   * @author Emilio Garcia
   */
  public record VehiclePaletteSpec(Integer maxSelectable, List<String> allowedComponents) {

    /**
     * Creates immutable vehicle-palette metadata.
     *
     * @author Emilio Garcia
     */
    public VehiclePaletteSpec {
      allowedComponents = allowedComponents == null ? List.of() : List.copyOf(allowedComponents);
    }
  }

  /**
   * Rectangular terrain block suggestion.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width block width
   * @param height block height
   * @param texture texture path
   * @param tileTexture true when the texture should tile
   * @param friction surface friction coefficient
   * @param restitution restitution coefficient
   * @author Emilio Garcia
   */
  public record TerrainBlockSpec(
      Double x,
      Double y,
      Double width,
      Double height,
      String texture,
      Boolean tileTexture,
      Double friction,
      Double restitution) {
  }

  /**
   * Quadratic spline terrain suggestion.
   *
   * @param knots spline knot list
   * @param controls spline control-point list
   * @param baselineY fill baseline below the curve
   * @param resolution tessellation resolution hint
   * @param texture texture path
   * @param tileTexture true when the texture should tile
   * @param friction surface friction coefficient
   * @param color fallback fill color
   * @author Emilio Garcia
   */
  public record TerrainSplineSpec(
      List<PointSpec> knots,
      List<PointSpec> controls,
      Double baselineY,
      Integer resolution,
      String texture,
      Boolean tileTexture,
      Double friction,
      String color) {

    /**
     * Creates immutable spline terrain data.
     *
     * @author Emilio Garcia
     */
    public TerrainSplineSpec {
      knots = knots == null ? List.of() : List.copyOf(knots);
      controls = controls == null ? List.of() : List.copyOf(controls);
    }
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

  /**
   * Rock obstacle suggestion.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width rock width
   * @param height rock height
   * @param friction rock friction coefficient
   * @param restitution rock restitution coefficient
   * @param damage contact damage amount
   * @author Emilio Garcia
   */
  public record RockSpec(
      Double x,
      Double y,
      Double width,
      Double height,
      Double friction,
      Double restitution,
      Double damage) {
  }

  /**
   * Pickup suggestion.
   *
   * @param type supported pickup type identifier
   * @param x x-coordinate
   * @param y y-coordinate
   * @param width pickup width
   * @param height pickup height
   * @param amount pickup amount or score value
   * @author Emilio Garcia
   */
  public record PickupSpec(
      String type,
      Double x,
      Double y,
      Double width,
      Double height,
      Double amount) {
  }
}
