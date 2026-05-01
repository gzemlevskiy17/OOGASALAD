package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.LevelLoadException;

/**
 * Validates a parsed level JSON against the new level schema.
 *
 * <p>Required structure:
 * <ul>
 *   <li>{@code gameMetadata}: object with levelName, gameType, positive screenWidth/screenHeight,
 *       finite gravity, drag in [0, 1]</li>
 *   <li>{@code map}: object with knotLocations (array of {x, y}), spawnPoint ({x, y} object),
 *       objects (array of {type, position})</li>
 *   <li>{@code mainCharacter}: object with positive gridSize, elementsAllowed (array),
 *       positive maxSelectable, positive maxHP</li>
 * </ul>
 *
 * <p>All problems are collected and reported in a single {@link LevelLoadException}, so a
 * level designer sees every error in one pass instead of fixing one issue per save.
 */
public class LevelValidator {

  private static final double DRAG_MIN = 0.0;
  private static final double DRAG_MAX = 1.0;
  private static final double GRAVITY_MIN = -50.0;
  private static final double GRAVITY_MAX = 0.0;

  public void validate(JsonNode root) {
    List<String> errors = new ArrayList<>();
    if (!isObject(root, "root", errors)) {
      throw new LevelLoadException(formatErrors(errors));
    }
    validateGameMetadata(root.path("gameMetadata"), errors);
    validateMap(root.path("map"), errors);
    validateMainCharacter(root.path("mainCharacter"), errors);
    if (!errors.isEmpty()) {
      throw new LevelLoadException(formatErrors(errors));
    }
  }

  private static String formatErrors(List<String> errors) {
    String problemLabel = " problems:";
    if (errors.size() == 1) {
      problemLabel = " problem:";
    }
    StringBuilder sb = new StringBuilder("Level file has ")
        .append(errors.size())
        .append(problemLabel);
    for (String error : errors) {
      sb.append("\n  - ").append(error);
    }
    return sb.toString();
  }

  // Section validators

  private void validateGameMetadata(JsonNode meta, List<String> errors) {
    if (!isObject(meta, "gameMetadata", errors)) {
      return;
    }
    requireText(meta, "levelName", "gameMetadata.levelName", errors);
    requireText(meta, "gameType", "gameMetadata.gameType", errors);
    requirePositive(meta, "screenWidth", "gameMetadata.screenWidth", errors);
    requirePositive(meta, "screenHeight", "gameMetadata.screenHeight", errors);
    requireGravity(meta, errors);
    requireInRange(meta, "drag", "gameMetadata.drag", DRAG_MIN, DRAG_MAX, errors);
  }

  private void validateMap(JsonNode map, List<String> errors) {
    if (!isObject(map, "map", errors)) {
      return;
    }
    JsonNode knots = map.path("knotLocations");
    if (isArray(knots, "map.knotLocations", errors)) {
      for (int i = 0; i < knots.size(); i++) {
        validatePoint(knots.get(i), "map.knotLocations[" + i + "]", errors);
      }
    }
    JsonNode objects = map.path("objects");
    if (isArray(objects, "map.objects", errors)) {
      for (int i = 0; i < objects.size(); i++) {
        validateMapObject(objects.get(i), "map.objects[" + i + "]", errors);
      }
    }
    JsonNode spawn = map.path("spawnPoint");
    if (spawn.isMissingNode() || !spawn.isObject()) {
      errors.add("map.spawnPoint must be a {x, y} object");
    } else {
      validatePoint(spawn, "map.spawnPoint", errors);
    }
  }

  private void validateMainCharacter(JsonNode mc, List<String> errors) {
    if (!isObject(mc, "mainCharacter", errors)) {
      return;
    }
    requirePositive(mc, "gridSize", "mainCharacter.gridSize", errors);
    isArray(mc.path("elementsAllowed"), "mainCharacter.elementsAllowed", errors);
    requirePositive(mc, "maxSelectable", "mainCharacter.maxSelectable", errors);
    requirePositive(mc, "maxHP", "mainCharacter.maxHP", errors);
  }

  private void validatePoint(JsonNode point, String path, List<String> errors) {
    if (point == null || !point.isObject()) {
      errors.add(path + " must be a {x, y} object");
      return;
    }
    requireFiniteNumber(point, "x", path + ".x", errors);
    requireFiniteNumber(point, "y", path + ".y", errors);
  }

  private void validateMapObject(JsonNode node, String path, List<String> errors) {
    if (node == null || !node.isObject()) {
      errors.add(path + " must be an object with 'type' and 'position'");
      return;
    }
    requireText(node, "type", path + ".type", errors);
    JsonNode pos = node.path("position");
    if (pos.isMissingNode() || !pos.isObject()) {
      errors.add(path + ".position must be a {x, y} object");
    } else {
      validatePoint(pos, path + ".position", errors);
    }
  }

  private void requireGravity(JsonNode meta, List<String> errors) {
    JsonNode field = meta.path("gravity");
    if (field.isMissingNode() || !field.isNumber()) {
      errors.add("missing required number: 'gameMetadata.gravity'");
      return;
    }
    double value = field.asDouble();
    if (!Double.isFinite(value) || value < GRAVITY_MIN || value > GRAVITY_MAX) {
      errors.add("gameMetadata.gravity must be in [" + GRAVITY_MIN + ", " + GRAVITY_MAX
          + "] (got " + field.asText() + "). Gravity uses negative values where "
          + GRAVITY_MIN + " is the strongest downward pull and 0 is no gravity.");
    }
  }

  // Field-level helpers

  private boolean isObject(JsonNode node, String path, List<String> errors) {
    if (node == null || node.isMissingNode() || !node.isObject()) {
      errors.add("missing required object: '" + path + "'");
      return false;
    }
    return true;
  }

  private boolean isArray(JsonNode node, String path, List<String> errors) {
    if (node == null || node.isMissingNode() || !node.isArray()) {
      errors.add("missing required array: '" + path + "'");
      return false;
    }
    return true;
  }

  private void requireText(JsonNode node, String key, String path, List<String> errors) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isTextual() || field.asText().isBlank()) {
      errors.add("missing required string: '" + path + "'");
    }
  }

  private void requireFiniteNumber(JsonNode node, String key, String path, List<String> errors) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isNumber()) {
      errors.add("missing required number: '" + path + "'");
      return;
    }
    double value = field.asDouble();
    if (!Double.isFinite(value)) {
      errors.add(path + " must be a finite number (got " + field.asText() + ")");
    }
  }

  private void requirePositive(JsonNode node, String key, String path, List<String> errors) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isNumber()) {
      errors.add("missing required number: '" + path + "'");
      return;
    }
    double value = field.asDouble();
    if (!Double.isFinite(value) || value <= 0) {
      errors.add(path + " must be a positive finite number (got " + field.asText() + ")");
    }
  }

  private void requireInRange(JsonNode node, String key, String path, double min, double max,
      List<String> errors) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isNumber()) {
      errors.add("missing required number: '" + path + "'");
      return;
    }
    double value = field.asDouble();
    if (!Double.isFinite(value) || value < min || value > max) {
      errors.add(path + " must be in [" + min + ", " + max + "] (got " + field.asText() + ")");
    }
  }
}
