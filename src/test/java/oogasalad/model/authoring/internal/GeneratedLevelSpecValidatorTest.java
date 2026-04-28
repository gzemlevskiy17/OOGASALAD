package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.junit.jupiter.api.Test;

/**
 * Tests generated-level schema validation against the fixed catalog.
 * @author Emilio Garcia
 */
class GeneratedLevelSpecValidatorTest {

  @Test
  void Validate_ValidGeneratedLevelSpec_DoesNotThrow() {
    GeneratedLevelSpecValidator validator = new GeneratedLevelSpecValidator(
        GeneratedLevelFixtures.provider(GeneratedLevelFixtures.catalog()));

    assertDoesNotThrow(() -> validator.validate(GeneratedLevelFixtures.validSpec()));
  }

  @Test
  void Validate_MapWithoutGoalLine_ThrowsGeneratedLevelSpecValidationException() {
    GeneratedLevelSpec validSpec = GeneratedLevelFixtures.validSpec();
    GeneratedLevelSpec invalidSpec = new GeneratedLevelSpec(
        validSpec.gameMetadata(),
        new GeneratedLevelSpec.MapSpec(
            validSpec.map().knotLocations(),
            validSpec.map().spawnPoint(),
            List.of(new GeneratedLevelSpec.MapObjectSpec(
                "Coin", new GeneratedLevelSpec.PointSpec(1200.0, 850.0))),
            validSpec.map().terrainOptions()),
        validSpec.mainCharacter());
    GeneratedLevelSpecValidator validator = new GeneratedLevelSpecValidator(
        GeneratedLevelFixtures.provider(GeneratedLevelFixtures.catalog()));

    assertThrows(GeneratedLevelSpecValidationException.class,
        () -> validator.validate(invalidSpec));
  }

  @Test
  void Constructor_NullCatalogProvider_ThrowsGeneratedLevelSpecValidationException() {
    assertThrows(GeneratedLevelSpecValidationException.class,
        () -> new GeneratedLevelSpecValidator(null));
  }
}
