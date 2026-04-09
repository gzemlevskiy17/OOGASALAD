package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oogasalad.data.api.LevelLoadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LevelValidatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private LevelValidator validator;

  @BeforeEach
  void setUp() {
    validator = new LevelValidator();
  }

  private JsonNode parse(String json) throws JsonProcessingException {
    return MAPPER.readTree(json);
  }

  // --- Positive cases ---

  @Test
  void validate_minimalValidLevel_passes() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0}, "size": {"width": 10, "height": 2} }
            ]
          }
        }
        """);

    assertDoesNotThrow(() -> validator.validate(root));
  }

  @Test
  void validate_multipleValidTerrainEntries_passes() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0},  "size": {"width": 5, "height": 1} },
              { "position": {"x": 5, "y": 0},  "size": {"width": 5, "height": 1} },
              { "position": {"x": 0, "y": 10}, "size": {"width": 3, "height": 3} }
            ]
          }
        }
        """);

    assertDoesNotThrow(() -> validator.validate(root));
  }

  @Test
  void validate_levelWithNoTerrainKey_passes() throws JsonProcessingException {
    JsonNode root = parse("{\"level\": {}}");

    assertDoesNotThrow(() -> validator.validate(root));
  }

  @Test
  void validate_emptyTerrainArray_passes() throws JsonProcessingException {
    JsonNode root = parse("{\"level\": {\"terrain\": []}}");

    assertDoesNotThrow(() -> validator.validate(root));
  }

  // --- Negative cases ---

  @Test
  void validate_missingLevelKey_throws() throws JsonProcessingException {
    JsonNode root = parse("{}");

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_levelIsString_throws() throws JsonProcessingException {
    JsonNode root = parse("{\"level\": \"oops\"}");

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_terrainIsObject_throws() throws JsonProcessingException {
    JsonNode root = parse("{\"level\": {\"terrain\": {}}}");

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_terrainEntryZeroWidth_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0}, "size": {"width": 0, "height": 2} }
            ]
          }
        }
        """);

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_terrainEntryNegativeHeight_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0}, "size": {"width": 5, "height": -1} }
            ]
          }
        }
        """);

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_terrainEntryMissingSize_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0, "y": 0} }
            ]
          }
        }
        """);

    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_zeroWidthMessageIncludesIndex() throws JsonProcessingException {
    JsonNode root = parse("""
        {
          "level": {
            "terrain": [
              { "position": {"x": 0,  "y": 0}, "size": {"width": 5, "height": 1} },
              { "position": {"x": 5,  "y": 0}, "size": {"width": 5, "height": 1} },
              { "position": {"x": 10, "y": 0}, "size": {"width": 0, "height": 2} }
            ]
          }
        }
        """);

    LevelLoadException thrown = assertThrows(
        LevelLoadException.class, () -> validator.validate(root));
    assertTrue(thrown.getMessage().contains("terrain[2]"),
        "Expected message to identify the offending index, got: " + thrown.getMessage());
  }
}
