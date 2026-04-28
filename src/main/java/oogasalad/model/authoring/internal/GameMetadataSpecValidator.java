package oogasalad.model.authoring.internal;

import oogasalad.model.authoring.api.GeneratedLevelSpec.GameMetadataSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;

/**
 * Validates the schema-native {@code gameMetadata} section for generated Good Piggies levels.
 * @author Emilio Garcia
 */
final class GameMetadataSpecValidator {

  private final GoodPiggiesCatalog catalog;
  private final ValidationSupport support;

  /**
   * @param catalog fixed Good Piggies catalog used to verify the level's game id
   * @param support shared validation utilities and properties
   */
  GameMetadataSpecValidator(GoodPiggiesCatalog catalog, ValidationSupport support) {
    this.catalog = catalog;
    this.support = support;
  }

  void validate(GameMetadataSpec metadata) {
    support.requirePresent("missingGameMetadataError", metadata);
    support.requireNonBlank("levelNameMissingError", metadata.levelName());
    support.requireNonBlank("gameTypeMissingError", metadata.gameType());
    if (!catalog.gameId().equals(metadata.gameType())) {
      support.fail("gameTypeInvalidError", metadata.gameType(), catalog.gameId());
    }
    support.validateRange("screenWidthOutOfRangeError", metadata.screenWidth(),
        support.d("minScreenWidth"), support.d("maxScreenWidth"));
    support.validateRange("screenHeightOutOfRangeError", metadata.screenHeight(),
        support.d("minScreenHeight"), support.d("maxScreenHeight"));
    support.validateRange("gravityOutOfRangeError", metadata.gravity(),
        support.d("minGravity"), support.d("maxGravity"));
    support.validateRange("dragOutOfRangeError", metadata.drag(),
        support.d("minDrag"), support.d("maxDrag"));
  }
}
