package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads game_config.json and exposes settings via dot-path keys.
 *
 * Keys are dot-separated paths into the JSON tree.
 * Example: getSetting("camera.mode") navigates root → "camera" → "mode".
 *
 * Collaborators: LevelLoaderImpl (reads available component types),
 *                AuthoringControllerImpl (reads build_mode grid dimensions),
 *                GameControllerImpl (reads input_mapping, win/loss config).
 */
public class SettingsImpl implements Settings {

  private static final Logger LOG = LogManager.getLogger(SettingsImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JsonNode root;

  public SettingsImpl(String filePath) {
    try {
      root = MAPPER.readTree(new File(filePath));
      LOG.info("Loaded game config from {}", filePath);
    } catch (IOException e) {
      throw new LevelLoadException("Failed to read game config: " + filePath, e);
    }
  }

  @Override
  public String getSetting(String key) {
    JsonNode node = resolve(key);
    return (node != null && !node.isMissingNode()) ? node.asText() : null;
  }

  @Override
  public double getNumericSetting(String key) {
    JsonNode node = resolve(key);
    if (node == null || node.isMissingNode()) {
      throw new IllegalArgumentException("No numeric setting found for key: " + key);
    }
    return node.asDouble();
  }

  /** Walks dot-separated segments down the JSON tree. Returns null if any segment is missing. */
  private JsonNode resolve(String key) {
    String[] parts = key.split("\\.");
    JsonNode current = root;
    for (String part : parts) {
      if (current == null || current.isMissingNode()) {
        return null;
      }
      current = current.get(part);
    }
    return current;
  }
}
