package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import oogasalad.data.api.LevelLoadException;

/**
 * Minimal structural validator for level JSON. Fails fast on the first problem it finds with a
 * descriptive {@link LevelLoadException}. Operates purely on the parsed {@link JsonNode}
 * tree — no World or engine state involved.
 *
 * <p>Current checks:
 * <ol>
 *   <li>Root has a {@code level} object.</li>
 *   <li>If present, {@code level.terrain} is an array (absent is allowed).</li>
 *   <li>If present, {@code level.rocks} is an array (absent is allowed).</li>
 *   <li>Each terrain or rock entry has positive {@code size.width} and {@code size.height}.</li>
 * </ol>
 */
public class LevelValidator {

  public void validate(JsonNode root) {
    JsonNode level = root.path("level");
    if (!level.isObject()) {
      throw new LevelLoadException("Level file missing required 'level' root object");
    }

    validateSizedArray(level.path("terrain"), "terrain");
    validateSizedArray(level.path("rocks"), "rocks");
  }

  private void validateSizedArray(JsonNode array, String fieldName) {
    if (array.isMissingNode()) {
      return;
    }
    if (!array.isArray()) {
      throw new LevelLoadException("'" + fieldName + "' must be an array");
    }
    for (int i = 0; i < array.size(); i++) {
      JsonNode entry = array.get(i);
      JsonNode size = entry.path("size");
      double width = size.path("width").asDouble(0);
      double height = size.path("height").asDouble(0);
      if (width <= 0 || height <= 0) {
        throw new LevelLoadException(fieldName + "[" + i + "] has non-positive size");
      }
    }
  }
}
