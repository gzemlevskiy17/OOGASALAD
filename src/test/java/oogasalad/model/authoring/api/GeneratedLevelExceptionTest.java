package oogasalad.model.authoring.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests custom generated-level exception constructors.
 * @author Emilio Garcia
 */
class GeneratedLevelExceptionTest {

  @Test
  void GeneratedLevelFileException_MessageProvided_StoresMessage() {
    GeneratedLevelFileException exception = new GeneratedLevelFileException("save failed");

    assertEquals("save failed", exception.getMessage());
  }

  @Test
  void GeneratedLevelJsonBuildException_CauseProvided_StoresCause() {
    RuntimeException rootCause = new RuntimeException("root");

    GeneratedLevelJsonBuildException exception =
        new GeneratedLevelJsonBuildException("build failed", rootCause);

    assertSame(rootCause, exception.getCause());
  }

  @Test
  void GeneratedLevelSpecValidationException_MessageProvided_StoresMessage() {
    GeneratedLevelSpecValidationException exception =
        new GeneratedLevelSpecValidationException("invalid spec");

    assertEquals("invalid spec", exception.getMessage());
  }

  @Test
  void GoodPiggiesCatalogException_CauseProvided_StoresCause() {
    RuntimeException rootCause = new RuntimeException("root");

    GoodPiggiesCatalogException exception =
        new GoodPiggiesCatalogException("catalog failed", rootCause);

    assertSame(rootCause, exception.getCause());
  }

  @Test
  void LevelGenerationConfigurationException_MessageProvided_StoresMessage() {
    LevelGenerationConfigurationException exception =
        new LevelGenerationConfigurationException("missing config");

    assertEquals("missing config", exception.getMessage());
  }

  @Test
  void LevelGenerationRequestException_CauseProvided_StoresCause() {
    RuntimeException rootCause = new RuntimeException("root");

    LevelGenerationRequestException exception =
        new LevelGenerationRequestException("request failed", rootCause);

    assertSame(rootCause, exception.getCause());
  }
}
