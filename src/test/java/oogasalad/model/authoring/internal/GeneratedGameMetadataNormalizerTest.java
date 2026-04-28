package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests normalization of generated-level metadata.
 * @author Emilio Garcia
 */
class GeneratedGameMetadataNormalizerTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void Normalize_NullMetadata_UsesDefaultMetadataValues() {
    GeneratedGameMetadataNormalizer normalizer = new GeneratedGameMetadataNormalizer(PROPS);

    GeneratedLevelSpec.GameMetadataSpec metadata = normalizer.normalize(null, "badpiggies");

    assertEquals("Generated Level", metadata.levelName());
    assertEquals("badpiggies", metadata.gameType());
    assertEquals(3200.0, metadata.screenWidth());
  }

  @Test
  void Normalize_OutOfRangePhysicsValues_ClampsValuesToSafeRange() {
    GeneratedGameMetadataNormalizer normalizer = new GeneratedGameMetadataNormalizer(PROPS);
    GeneratedLevelSpec.GameMetadataSpec unsafeMetadata =
        new GeneratedLevelSpec.GameMetadataSpec("Unsafe", "other", 1.0, 1.0, -100.0, 2.0);

    GeneratedLevelSpec.GameMetadataSpec metadata =
        normalizer.normalize(unsafeMetadata, "badpiggies");

    assertEquals(800.0, metadata.screenWidth());
    assertEquals(600.0, metadata.screenHeight());
    assertEquals(-20.0, metadata.gravity());
    assertEquals(1.0, metadata.drag());
  }
}
