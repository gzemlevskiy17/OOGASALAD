package oogasalad.model.authoring.internal;

import java.util.Set;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;

/**
 * Orchestrates schema-native validation of AI-generated Good Piggies levels.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelSpecValidator {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecValidator");

  private final ValidationSupport support;
  private final GameMetadataSpecValidator gameMetadataValidator;
  private final MapSpecValidator mapValidator;
  private final MainCharacterSpecValidator mainCharacterValidator;

  /**
   * Creates a generated-level validator using the fixed Good Piggies catalog provider.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelSpecValidator() {
    this(new GoodPiggiesCatalogProvider());
  }

  /**
   * Creates a generated-level validator with an injected Good Piggies catalog provider.
   *
   * @param catalogProvider provider for the fixed Good Piggies catalog
   * @author Emilio Garcia
   */
  public GeneratedLevelSpecValidator(GoodPiggiesCatalogProvider catalogProvider) {
    this.support = new ValidationSupport(PROPS);
    if (catalogProvider == null) {
      support.fail("nullCatalogProviderError");
    }
    GoodPiggiesCatalog catalog = catalogProvider.loadCatalog();
    Set<String> allowedComponentIds =
        catalog.componentIds().stream().collect(Collectors.toUnmodifiableSet());
    Set<String> allowedStaticObjectIds =
        catalog.staticComponentIds().stream().collect(Collectors.toUnmodifiableSet());
    Set<String> allowedTerrainIds =
        catalog.terrainTypeIds().stream().collect(Collectors.toUnmodifiableSet());
    this.gameMetadataValidator = new GameMetadataSpecValidator(catalog, support);
    this.mapValidator = new MapSpecValidator(allowedStaticObjectIds, allowedTerrainIds, support);
    this.mainCharacterValidator =
        new MainCharacterSpecValidator(catalog, allowedComponentIds, support);
  }

  /**
   * Validates the provided generated-level spec and throws on the first unsafe or
   * schema-incompatible value.
   *
   * @param spec generated level specification to validate
   * @author Emilio Garcia
   */
  public void validate(GeneratedLevelSpec spec) {
    support.requirePresent("nullSpecError", spec);
    gameMetadataValidator.validate(spec.gameMetadata());
    mapValidator.validate(spec.map());
    mainCharacterValidator.validate(spec.mainCharacter());
  }
}
