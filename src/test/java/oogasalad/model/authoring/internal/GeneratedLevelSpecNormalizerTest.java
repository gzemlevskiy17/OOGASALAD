package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.junit.jupiter.api.Test;

/**
 * Tests generated-level normalization into playable Good Piggies defaults.
 *
 * @author Emilio Garcia
 */
class GeneratedLevelSpecNormalizerTest {

  @Test
  void Normalize_SparseGeneratedLevelSpec_FillsDefaultsAndSynthesizesGoalLine() {
    GeneratedLevelSpecNormalizer normalizer = new GeneratedLevelSpecNormalizer(
        GeneratedLevelFixtures.provider(GeneratedLevelFixtures.catalog()));

    GeneratedLevelSpec normalized = normalizer.normalize(new GeneratedLevelSpec(null, null, null));

    assertEquals("badpiggies", normalized.gameMetadata().gameType());
    assertTrue(normalized.map().objects().stream()
        .anyMatch(object -> "GoalLine".equals(object.type())));
    assertFalse(normalized.map().terrainOptions().isEmpty());
  }

  @Test
  void Normalize_InvalidComponentInElementsAllowed_DropsUnsupportedComponent() {
    GeneratedLevelSpecNormalizer normalizer = new GeneratedLevelSpecNormalizer(
        GeneratedLevelFixtures.provider(GeneratedLevelFixtures.catalog()));
    GeneratedLevelSpec validSpec = GeneratedLevelFixtures.validSpec();
    GeneratedLevelSpec specWithInvalidComponent = new GeneratedLevelSpec(
        validSpec.gameMetadata(),
        validSpec.map(),
        new GeneratedLevelSpec.MainCharacterSpec(
            60.0, java.util.List.of("Frame", "MissingPart"), 5, 100.0, "vehicle"));

    GeneratedLevelSpec normalized = normalizer.normalize(specWithInvalidComponent);

    assertEquals(java.util.List.of("Frame"), normalized.mainCharacter().elementsAllowed());
  }

  @Test
  void Normalize_NullGeneratedLevelSpec_ThrowsGeneratedLevelSpecValidationException() {
    GeneratedLevelSpecNormalizer normalizer = new GeneratedLevelSpecNormalizer(
        GeneratedLevelFixtures.provider(GeneratedLevelFixtures.catalog()));

    assertThrows(GeneratedLevelSpecValidationException.class, () -> normalizer.normalize(null));
  }
}
