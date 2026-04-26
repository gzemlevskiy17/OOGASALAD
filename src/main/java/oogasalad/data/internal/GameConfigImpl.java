package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses a {@code game.json} file into a typed {@link GameConfig}.
 *
 * <p>Typed accessors reflect the new game schema. The legacy dot-path
 * {@link #getSetting} / {@link #getNumericSetting} methods still work for
 * any keys that exist in the raw JSON (e.g. {@code input_mapping.*}).
 */
public class GameConfigImpl implements GameConfig {

  private static final Logger LOG = LogManager.getLogger(GameConfigImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JsonNode root;
  private final String gameId;
  private final String primaryComponentId;
  private final List<ComponentInfo> components;
  private final List<StaticComponentInfo> staticComponents;
  private final List<TerrainInfo> terrainTypes;

  public GameConfigImpl(String filePath) {
    try {
      root = MAPPER.readTree(new File(filePath));
      LOG.info("Loaded game config from {}", filePath);
    } catch (IOException e) {
      throw new LevelLoadException("Failed to read game config: " + filePath, e);
    }
    gameId = root.path("gameId").asText("");
    primaryComponentId = root.path("primaryComponentId").asText("");
    components = parseComponents(root.path("components"));
    staticComponents = parseStaticComponents(root.path("staticComponents"));
    terrainTypes = parseTerrainTypes(root.path("terrainTypes"));
  }

  /** Creates an empty in-memory catalog (used before a game file is loaded). */
  public static GameConfigImpl empty() {
    return new GameConfigImpl();
  }

  private GameConfigImpl() {
    root = MAPPER.createObjectNode();
    gameId = "";
    primaryComponentId = "";
    components = new ArrayList<>();
    staticComponents = new ArrayList<>();
    terrainTypes = new ArrayList<>();
  }

  @Override public String gameId() { return gameId; }
  @Override public String primaryComponentId() { return primaryComponentId; }
  @Override public List<ComponentInfo> components() { return List.copyOf(components); }
  @Override public List<StaticComponentInfo> staticComponents() { return List.copyOf(staticComponents); }
  @Override public List<TerrainInfo> terrainTypes() { return List.copyOf(terrainTypes); }

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

  private List<ComponentInfo> parseComponents(JsonNode array) {
    List<ComponentInfo> result = new ArrayList<>();
    if (!array.isArray()) return result;
    for (JsonNode n : array) {
      String id = n.path("id").asText("");
      String sprite = n.path("spriteUrl").asText("");
      double mass = n.path("mass").asDouble(1.0);
      List<ButtonInfo> buttons = parseButtons(n.path("buttons"));
      result.add(new ComponentInfo(id, sprite, mass, buttons));
    }
    return result;
  }

  private List<ButtonInfo> parseButtons(JsonNode array) {
    List<ButtonInfo> result = new ArrayList<>();
    if (!array.isArray()) return result;
    for (JsonNode b : array) {
      String name = b.path("name").asText("");
      double dx = b.path("direction").path("x").asDouble(0);
      double dy = b.path("direction").path("y").asDouble(0);
      result.add(new ButtonInfo(name, dx, dy));
    }
    return result;
  }

  private List<StaticComponentInfo> parseStaticComponents(JsonNode array) {
    List<StaticComponentInfo> result = new ArrayList<>();
    if (!array.isArray()) return result;
    for (JsonNode n : array) {
      String id = n.path("id").asText("");
      String sprite = n.path("spriteUrl").asText("");
      double mass = n.path("mass").asDouble(0);
      CollisionAction action = parseCollisionAction(n.path("onCollisionAction"));
      result.add(new StaticComponentInfo(id, sprite, mass, action));
    }
    return result;
  }

  private CollisionAction parseCollisionAction(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) return null;
    String type = node.path("type").asText("");
    double amount = node.path("amount").asDouble(0);
    return new CollisionAction(type, amount);
  }

  private List<TerrainInfo> parseTerrainTypes(JsonNode array) {
    List<TerrainInfo> result = new ArrayList<>();
    if (!array.isArray()) return result;
    for (JsonNode n : array) {
      String id = n.path("id").asText("");
      String sprite = n.path("spriteUrl").isNull() ? null : n.path("spriteUrl").asText(null);
      String color = n.path("visualColor").isNull() ? null : n.path("visualColor").asText(null);
      double friction = n.path("friction").asDouble(0.5);
      result.add(new TerrainInfo(id, sprite, color, friction));
    }
    return result;
  }

  private JsonNode resolve(String key) {
    String[] parts = key.split("\\.");
    JsonNode current = root;
    for (String part : parts) {
      if (current == null || current.isMissingNode()) return null;
      current = current.get(part);
    }
    return current;
  }
}
