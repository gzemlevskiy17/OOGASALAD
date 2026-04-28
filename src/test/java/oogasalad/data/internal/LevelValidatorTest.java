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

  // ── Range checks ─────────────────────────────────────────────────────────

  @Test
  void validate_zeroScreenWidth_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"screenWidth\": 3200", "\"screenWidth\": 0"));
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("gameMetadata.screenWidth"));
  }

  @Test
  void validate_negativeScreenHeight_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"screenHeight\": 1600", "\"screenHeight\": -10"));
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_dragAboveOne_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"drag\": 0.01", "\"drag\": 1.5"));
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("gameMetadata.drag"));
  }

  @Test
  void validate_dragBelowZero_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"drag\": 0.01", "\"drag\": -0.1"));
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_zeroMaxHP_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"maxHP\": 100", "\"maxHP\": 0"));
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("mainCharacter.maxHP"));
  }

  @Test
  void validate_zeroGridSize_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"gridSize\": 60", "\"gridSize\": 0"));
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_zeroMaxSelectable_throws() throws JsonProcessingException {
    JsonNode root = parse(VALID_LEVEL.replace("\"maxSelectable\": 5", "\"maxSelectable\": 0"));
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  // ── Per-element shape checks ─────────────────────────────────────────────

  @Test
  void validate_knotLocationMissingY_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [ { "x": 1.0 } ],
                   "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("map.knotLocations[0].y"));
  }

  @Test
  void validate_mapObjectMissingPosition_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0},
                   "objects": [ { "type": "Rock" } ] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("map.objects[0].position"));
  }

  @Test
  void validate_mapObjectMissingType_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0},
                   "objects": [ { "position": {"x":1,"y":2} } ] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    assertThrows(LevelLoadException.class, () -> validator.validate(root));
  }

  @Test
  void validate_spawnPointMissingX_throws() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 100, "screenHeight": 100, "gravity": -9.8, "drag": 0.01 },
          "map": { "knotLocations": [], "spawnPoint": { "y": 0 }, "objects": [] },
          "mainCharacter": { "gridSize": 60, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("map.spawnPoint.x"));
  }

  // ── Multi-error reporting ────────────────────────────────────────────────

  @Test
  void validate_multipleErrors_aggregatedInOneMessage() throws JsonProcessingException {
    JsonNode root = parse("""
        { "gameMetadata": { "levelName": "T", "gameType": "g",
            "screenWidth": 0, "screenHeight": -1, "gravity": -9.8, "drag": 5.0 },
          "map": { "knotLocations": [], "spawnPoint": {"x":0,"y":0}, "objects": [] },
          "mainCharacter": { "gridSize": 0, "elementsAllowed": [], "maxSelectable": 5, "maxHP": 100 } }
        """);
    LevelLoadException ex = assertThrows(LevelLoadException.class,
        () -> validator.validate(root));
    assertTrue(ex.getMessage().contains("screenWidth"),
        "should mention screenWidth: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("screenHeight"),
        "should mention screenHeight: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("drag"),
        "should mention drag: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("gridSize"),
        "should mention gridSize: " + ex.getMessage());
  }
}
