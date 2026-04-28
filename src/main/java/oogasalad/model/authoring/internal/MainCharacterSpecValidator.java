package oogasalad.model.authoring.internal;

import java.util.List;
import java.util.Set;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MainCharacterSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;

/**
 * Validates the schema-native {@code mainCharacter} section for generated Good Piggies levels.
 * @author Emilio Garcia
 */
final class MainCharacterSpecValidator {

  private final GoodPiggiesCatalog catalog;
  private final Set<String> allowedComponentIds;
  private final ValidationSupport support;

  /**
   * @param catalog fixed Good Piggies catalog used to verify camera-follow behavior
   * @param allowedComponentIds valid Good Piggies buildable component ids
   * @param support shared validation utilities and properties
   */
  MainCharacterSpecValidator(GoodPiggiesCatalog catalog, Set<String> allowedComponentIds,
      ValidationSupport support) {
    this.catalog = catalog;
    this.allowedComponentIds = allowedComponentIds;
    this.support = support;
  }

  void validate(MainCharacterSpec mainCharacter) {
    support.requirePresent("missingMainCharacterError", mainCharacter);
    support.validateRange("gridSizeOutOfRangeError", mainCharacter.gridSize(),
        support.d("minGridSize"), support.d("maxGridSize"));
    validateComponentIds(mainCharacter.elementsAllowed());
    support.validateIntegerRange("maxSelectableOutOfRangeError", mainCharacter.maxSelectable(),
        support.n("minMaxSelectable"), support.n("maxMaxSelectable"));
    support.validateRange("maxHpOutOfRangeError", mainCharacter.maxHP(),
        support.d("minMaxHp"), support.d("maxMaxHp"));
    validateCameraFollows(mainCharacter.cameraFollows());
  }

  private void validateComponentIds(List<String> componentIds) {
    if (componentIds == null) {
      support.fail("elementsAllowedMissingError");
    }
    for (int i = 0; i < componentIds.size(); i++) {
      String componentId = componentIds.get(i);
      support.requireNonBlank("componentIdMissingError", componentId, i);
      if (!allowedComponentIds.contains(componentId)) {
        support.fail("componentIdInvalidError", componentId, i);
      }
    }
  }

  private void validateCameraFollows(String cameraFollows) {
    if (cameraFollows == null || cameraFollows.isBlank()) {
      return;
    }
    if (!cameraFollows.equals(catalog.mainCharacterId())
        && !cameraFollows.equals(support.s("defaultCameraFollows"))) {
      support.fail("cameraFollowsInvalidError", cameraFollows, catalog.mainCharacterId(),
          support.s("defaultCameraFollows"));
    }
  }
}
