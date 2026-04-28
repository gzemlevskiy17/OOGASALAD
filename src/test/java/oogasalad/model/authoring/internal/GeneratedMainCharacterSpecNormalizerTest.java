package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests normalization of generated main-character metadata.
 * @author Emilio Garcia
 */
class GeneratedMainCharacterSpecNormalizerTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void Normalize_NullMainCharacter_UsesDefaultBuildSettings() {
    GeneratedMainCharacterSpecNormalizer normalizer =
        new GeneratedMainCharacterSpecNormalizer(PROPS);

    GeneratedLevelSpec.MainCharacterSpec mainCharacter =
        normalizer.normalize(null, GeneratedLevelFixtures.catalog());

    assertEquals(60.0, mainCharacter.gridSize());
    assertEquals(5, mainCharacter.maxSelectable());
    assertEquals("vehicle", mainCharacter.cameraFollows());
  }

  @Test
  void Normalize_DuplicateAndInvalidComponentIds_KeepsUniqueCatalogComponentsOnly() {
    GeneratedMainCharacterSpecNormalizer normalizer =
        new GeneratedMainCharacterSpecNormalizer(PROPS);
    GeneratedLevelSpec.MainCharacterSpec generatedMainCharacter =
        new GeneratedLevelSpec.MainCharacterSpec(
            60.0, List.of("Frame", "Frame", "MissingPart", "BigPig"), 5, 100.0, "ignored");

    GeneratedLevelSpec.MainCharacterSpec mainCharacter =
        normalizer.normalize(generatedMainCharacter, GeneratedLevelFixtures.catalog());

    assertEquals(List.of("Frame", "BigPig"), mainCharacter.elementsAllowed());
    assertEquals("vehicle", mainCharacter.cameraFollows());
  }
}
