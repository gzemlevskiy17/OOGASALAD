package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes AI-generated schema-aligned level specifications into a safe Good Piggies level
 * shape before JSON serialization.
 *
 * <p>The normalizer fills defaults from the schema-era authoring contract, clamps physics values
 * into safe ranges, synthesizes a minimal knot track when missing, and guarantees the map contains
 * at least one finish object and one terrain option from the fixed Good Piggies catalog.
 * @author Emilio Garcia
 */
public class GeneratedLevelSpecNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelSpecNormalizer.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  private final GoodPiggiesCatalogProvider catalogProvider;
  private final GeneratedGameMetadataNormalizer gameMetadataNormalizer;
  private final GeneratedMapSpecNormalizer mapNormalizer;
  private final GeneratedMainCharacterSpecNormalizer mainCharacterNormalizer;

  /**
   * Creates a generated-level normalizer using the fixed Good Piggies catalog and resource-backed
   * normalization defaults.
   */
  public GeneratedLevelSpecNormalizer() {
    this(new GoodPiggiesCatalogProvider());
  }

  /**
   * Creates a generated-level normalizer with an injected Good Piggies catalog provider.
   *
   * @param catalogProvider provider for the fixed Good Piggies catalog
   */
  public GeneratedLevelSpecNormalizer(GoodPiggiesCatalogProvider catalogProvider) {
    if (catalogProvider == null) {
      String message = PROPS.getString("nullCatalogProviderError");
      LOG.error(message);
      throw new GeneratedLevelSpecValidationException(message);
    }
    this.catalogProvider = catalogProvider;
    this.gameMetadataNormalizer = new GeneratedGameMetadataNormalizer(PROPS);
    this.mapNormalizer = new GeneratedMapSpecNormalizer(PROPS);
    this.mainCharacterNormalizer = new GeneratedMainCharacterSpecNormalizer(PROPS);
  }

  /**
   * Returns a normalized copy of the provided generated-level spec.
   *
   * @param spec generated level specification to normalize
   * @return normalized Good Piggies level specification
   */
  public GeneratedLevelSpec normalize(GeneratedLevelSpec spec) {
    if (spec == null) {
      String message = PROPS.getString("nullSpecError");
      LOG.error(message);
      throw new GeneratedLevelSpecValidationException(message);
    }

    var catalog = catalogProvider.load();
    GeneratedLevelSpec.GameMetadataSpec normalizedMetadata =
        gameMetadataNormalizer.normalize(spec.gameMetadata(), catalog.gameId());
    GeneratedLevelSpec.MapSpec normalizedMap =
        mapNormalizer.normalize(
            spec.map(),
            normalizedMetadata.screenWidth(),
            normalizedMetadata.screenHeight(),
            catalog);
    GeneratedLevelSpec.MainCharacterSpec normalizedMainCharacter =
        mainCharacterNormalizer.normalize(spec.mainCharacter(), catalog);

    return new GeneratedLevelSpec(normalizedMetadata, normalizedMap, normalizedMainCharacter);
  }
}
