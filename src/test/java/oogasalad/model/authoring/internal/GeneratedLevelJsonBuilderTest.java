package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import oogasalad.model.authoring.api.GeneratedLevelJsonBuildException;
import org.junit.jupiter.api.Test;

/**
 * Tests conversion from normalized generated-level specs to runtime JSON.
 *
 * @author Emilio Garcia
 */
class GeneratedLevelJsonBuilderTest {

  @Test
  void Build_ValidGeneratedLevelSpec_WritesGameMetadataMapAndMainCharacterSections() {
    GeneratedLevelJsonBuilder builder = new GeneratedLevelJsonBuilder();

    ObjectNode levelJson = builder.build(GeneratedLevelFixtures.validSpec());

    assertEquals("Hill Run", levelJson.path("gameMetadata").path("levelName").asText());
    assertEquals("GoalLine", levelJson.path("map").path("objects").get(0).path("type").asText());
    assertEquals("Frame",
        levelJson.path("mainCharacter").path("elementsAllowed").get(0).asText());
  }

  @Test
  void Build_NullGeneratedLevelSpec_ThrowsGeneratedLevelJsonBuildException() {
    GeneratedLevelJsonBuilder builder = new GeneratedLevelJsonBuilder();

    assertThrows(GeneratedLevelJsonBuildException.class, () -> builder.build(null));
  }
}
