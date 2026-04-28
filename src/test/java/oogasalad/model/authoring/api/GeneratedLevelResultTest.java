package oogasalad.model.authoring.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests generated-level result value object behavior.
 * @author Emilio Garcia
 */
class GeneratedLevelResultTest {

  @Test
  void Constructor_FilePathAndGravityProvided_StoresValues() {
    GeneratedLevelResult result = new GeneratedLevelResult("/tmp/level.json", -9.8);

    assertEquals("/tmp/level.json", result.filePath());
    assertEquals(-9.8, result.gravity());
  }
}
