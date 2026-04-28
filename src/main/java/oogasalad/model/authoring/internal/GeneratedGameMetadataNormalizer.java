package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;

/**
 * Normalizes the schema {@code gameMetadata} section for AI-generated Good Piggies levels.
 * @author Emilio Garcia
 */
final class GeneratedGameMetadataNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a metadata normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   */
  GeneratedGameMetadataNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns a normalized {@code gameMetadata} section using the fixed Good Piggies game id.
   *
   * @param metadata generated metadata section
   * @param fixedGameId required Good Piggies game id
   * @return normalized schema metadata
   */
  GeneratedLevelSpec.GameMetadataSpec normalize(GeneratedLevelSpec.GameMetadataSpec metadata,
      String fixedGameId) {
    GeneratedLevelSpec.GameMetadataSpec safeMetadata =
        metadata == null ? new GeneratedLevelSpec.GameMetadataSpec(null, null, null, null, null,
            null) : metadata;
    return new GeneratedLevelSpec.GameMetadataSpec(
        support.normalizeText(safeMetadata.levelName(), support.s("defaultLevelName")),
        fixedGameId,
        support.clamp(support.defaultIfNull(safeMetadata.screenWidth(),
                support.d("defaultScreenWidth")),
            support.d("minScreenWidth"),
            support.d("maxScreenWidth")),
        support.clamp(support.defaultIfNull(safeMetadata.screenHeight(),
                support.d("defaultScreenHeight")),
            support.d("minScreenHeight"),
            support.d("maxScreenHeight")),
        support.clamp(support.defaultIfNull(safeMetadata.gravity(), support.d("defaultGravity")),
            support.d("minGravity"),
            support.d("maxGravity")),
        support.clamp(support.defaultIfNull(safeMetadata.drag(), support.d("defaultDrag")),
            support.d("minDrag"),
            support.d("maxDrag")));
  }
}
