package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oogasalad.data.api.LevelLoadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LevelValidatorTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private LevelValidator validator;

  private static final String VALID_LEVEL = """
      {
        "gameMetadata": {
          "levelName": "Test", "gameType": "badpiggies",
          "screenWidth": 3200, "screenHeight": 1600,
          "gravity": -9.8, "drag": 0.01
        },
        "map": {
          "knotLocations": [],
          "spawnPoint": { "x": 350, "y": 300 },
          "objects": []
        },
        "mainCharacter": {
          "gridSize": 60, "elementsAllowed": [],
          "maxSelectable": 5, "maxHP": 100
        }
      }
      """;

  @BeforeEach
  void setUp() {
    validator = new LevelValidator();
  }

  private JsonNode parse(String json) throws JsonProcessingException {
    return MAPPER.readTree(json);
  }

  // ── Positive cases ───────────────────────────────────────────────────────

  @Test
  void validate_completeValidLevel_passes() throws JsonProcessingException {
    assertDoesNotThrow(() -> validator.validate(parse(VALID_LEVEL)));
  }

  // ── Missing gameMetadata fields ──────────────────────────────────────────

  @Test
  void validate_missingGameMetadata_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingGameType_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "screenWidth": 100, "screenHeight": 100,
            "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingGravity_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  // ── Missing map fields ───────────────────────────────────────────────────

  @Test
  void validate_missingMap_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingKnotLocations_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingSpawnPoint_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  // ── Missing mainCharacter fields ─────────────────────────────────────────

  @Test
  void validate_missingMainCharacter_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingMaxHP_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_missingGridSize_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }
}
