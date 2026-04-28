package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import oogasalad.data.api.LevelLoadException;

/**
 * Validates a parsed level JSON against the new level schema.
 *
 * <p>Required structure:
 * <ul>
 *   <li>{@code gameMetadata}: object with levelName, gameType, screenWidth, screenHeight,
 *       gravity, drag</li>
 *   <li>{@code map}: object with knotLocations (array), spawnPoint (object), objects (array)</li>
 *   <li>{@code mainCharacter}: object with gridSize, elementsAllowed, maxSelectable, maxHP</li>
 * </ul>
 */
public class LevelValidator {

  public void validate(JsonNode root) {
    requireObject(root, "root");
    validateGameMetadata(root.path("gameMetadata"));
    validateMap(root.path("map"));
    validateMainCharacter(root.path("mainCharacter"));
  }

  private void validateGameMetadata(JsonNode meta) {
    requireObject(meta, "gameMetadata");
    requireText(meta, "levelName", "gameMetadata.levelName");
    requireText(meta, "gameType", "gameMetadata.gameType");
    requireNumber(meta, "screenWidth", "gameMetadata.screenWidth");
    requireNumber(meta, "screenHeight", "gameMetadata.screenHeight");
    requireNumber(meta, "gravity", "gameMetadata.gravity");
    requireNumber(meta, "drag", "gameMetadata.drag");
  }

  private void validateMap(JsonNode map) {
    requireObject(map, "map");
    requireArray(map, "knotLocations", "map.knotLocations");
    requireArray(map, "objects", "map.objects");
    validatePointArray(map.path("knotLocations"), "map.knotLocations");
    validatePoint(map.path("spawnPoint"), "map.spawnPoint");
    validateMapObjects(map.path("objects"));
  }

  private void validateMainCharacter(JsonNode mc) {
    requireObject(mc, "mainCharacter");
    requireNumber(mc, "gridSize", "mainCharacter.gridSize");
    requireArray(mc, "elementsAllowed", "mainCharacter.elementsAllowed");
    requireNumber(mc, "maxSelectable", "mainCharacter.maxSelectable");
    requireNumber(mc, "maxHP", "mainCharacter.maxHP");
  }

  private void requireObject(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode() || !node.isObject()) {
      throw new LevelLoadException("Level file missing required object: '" + fieldName + "'");
    }
  }

  private void requireText(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isTextual() || field.asText().isBlank()) {
      throw new LevelLoadException("Level file missing required string: '" + path + "'");
    }
  }

  private void requireNumber(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isNumber()) {
      throw new LevelLoadException("Level file missing required number: '" + path + "'");
    }
  }

  private void requireArray(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isArray()) {
      throw new LevelLoadException("Level file missing required array: '" + path + "'");
    }
  }

  private void validatePointArray(JsonNode array, String path) {
    for (int index = 0; index < array.size(); index++) {
      validatePoint(array.get(index), path + "[" + index + "]");
    }
  }

  private void validateMapObjects(JsonNode objects) {
    for (int index = 0; index < objects.size(); index++) {
      JsonNode object = objects.get(index);
      String path = "map.objects[" + index + "]";
      requireObject(object, path);
      requireText(object, "type", path + ".type");
      validatePoint(object.path("position"), path + ".position");
    }
  }

  private void validatePoint(JsonNode point, String path) {
    requireObject(point, path);
    requireNumber(point, "x", path + ".x");
    requireNumber(point, "y", path + ".y");
  }
}
