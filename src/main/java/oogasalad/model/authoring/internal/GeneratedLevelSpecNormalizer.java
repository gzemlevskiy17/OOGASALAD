package oogasalad.model.authoring.internal;

import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainBlockSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes AI-generated level specifications into a safe, fully populated intermediate form
 * before final JSON conversion.
 *
 * <p>The normalizer fills defaults, clamps physics values into sane ranges, repairs malformed
 * spline control data, and guarantees that at least one terrain structure exists so later builder
 * stages do not have to synthesize world geometry themselves.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelSpecNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelSpecNormalizer.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  private final LevelMetadataNormalizer metadataNormalizer;
  private final VehiclePaletteNormalizer vehiclePaletteNormalizer;
  private final TerrainBlockSpecNormalizer terrainBlockNormalizer;
  private final TerrainSplineSpecNormalizer terrainSplineNormalizer;
  private final RockSpecNormalizer rockNormalizer;
  private final PickupSpecNormalizer pickupNormalizer;
  private final DefaultTerrainSynthesizer terrainSynthesizer;

  /**
   * Creates a generated-level normalizer using resource-backed defaults and clamp ranges.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelSpecNormalizer() {
    this.metadataNormalizer = new LevelMetadataNormalizer(PROPS);
    this.vehiclePaletteNormalizer = new VehiclePaletteNormalizer(PROPS);
    this.terrainBlockNormalizer = new TerrainBlockSpecNormalizer(PROPS);
    this.terrainSplineNormalizer = new TerrainSplineSpecNormalizer(PROPS);
    this.rockNormalizer = new RockSpecNormalizer(PROPS);
    this.pickupNormalizer = new PickupSpecNormalizer(PROPS);
    this.terrainSynthesizer = new DefaultTerrainSynthesizer(PROPS);
  }

  /**
   * Returns a normalized copy of the provided generated-level spec.
   *
   * @param spec generated level specification to normalize
   * @return normalized generated level specification
   * @author Emilio Garcia
   */
  public GeneratedLevelSpec normalize(GeneratedLevelSpec spec) {
    if (spec == null) {
      String message = PROPS.getString("nullSpecError");
      LOG.error(message);
      throw new GeneratedLevelSpecValidationException(message);
    }

    NormalizedLevelMetadata metadata = metadataNormalizer.normalize(
        spec.style(),
        spec.levelName(),
        spec.gravity(),
        spec.backgroundImage(),
        spec.worldWidth(),
        spec.spawn(),
        spec.finishLine());

    List<TerrainBlockSpec> terrainBlocks = terrainBlockNormalizer.normalize(spec.terrainBlocks(),
        metadata.worldWidth());
    List<oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainSplineSpec> terrainSplines =
        terrainSplineNormalizer.normalize(spec.terrainSplines());
    if (terrainBlocks.isEmpty() && terrainSplines.isEmpty()) {
      terrainBlocks = List.of(terrainSynthesizer.synthesize(metadata.worldWidth()));
    }

    List<oogasalad.model.authoring.api.GeneratedLevelSpec.RockSpec> rocks =
        rockNormalizer.normalize(spec.rocks());
    List<oogasalad.model.authoring.api.GeneratedLevelSpec.PickupSpec> pickups =
        pickupNormalizer.normalize(spec.pickups());

    return new GeneratedLevelSpec(
        metadata.style(),
        metadata.levelName(),
        metadata.gravity(),
        metadata.backgroundImage(),
        metadata.worldWidth(),
        metadata.spawn(),
        metadata.finishLine(),
        vehiclePaletteNormalizer.normalize(spec.vehiclePalette()),
        terrainBlocks,
        terrainSplines,
        rocks,
        pickups);
  }
}
