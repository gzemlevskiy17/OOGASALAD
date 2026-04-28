package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
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
 * @author Paulina Vvedenskaya
 */
public class GameConfigImpl implements GameConfig {

  private static final Logger LOG = LogManager.getLogger(GameConfigImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JsonNode root;
  private final String gameId;
  private final String mainCharacterId;
  private final List<ComponentInfo> components;
  private final List<StaticComponentInfo> staticComponents;
  private final List<TerrainInfo> terrainTypes;
  private final List<CheatInfo> cheats;

  public GameConfigImpl(String filePath) {
    try {
      root = MAPPER.readTree(new File(filePath));
      LOG.info("Loaded game config from {}", filePath);
    } catch (IOException e) {
      throw new LevelLoadException("Failed to read game config: " + filePath, e);
    }
    gameId = root.path("gameId").asText("");
    mainCharacterId = root.path("mainCharacterId").asText("");
    components = parseComponents(root.path("components"));
    staticComponents = parseStaticComponents(root.path("staticComponents"));
    terrainTypes = parseTerrainTypes(root.path("terrainTypes"));
    cheats = parseCheats(root.path("cheats"));
  }

  private GameConfigImpl() {
    root = MAPPER.createObjectNode();
    gameId = "";
    mainCharacterId = "";
    components = new ArrayList<>();
    staticComponents = new ArrayList<>();
    terrainTypes = new ArrayList<>();
    cheats = new ArrayList<>();
  }

  public static GameConfigImpl empty() {
    return new GameConfigImpl();
  }

  @Override
  public String gameId() {
    return gameId;
  }

  @Override
  public String mainCharacterId() {
    return mainCharacterId;
  }

  @Override
  public List<ComponentInfo> components() {
    return List.copyOf(components);
  }

  @Override
  public List<StaticComponentInfo> staticComponents() {
    return List.copyOf(staticComponents);
  }

  @Override
  public List<TerrainInfo> terrainTypes() {
    return List.copyOf(terrainTypes);
  }

  @Override
  public List<CheatInfo> cheats() {
    return List.copyOf(cheats);
  }

  @Override
  public String getSetting(String key) {
    JsonNode node = resolve(key);
    if (node == null || node.isMissingNode()) {
      return null;
    }
    if (node.isArray()) {
      List<String> values = new ArrayList<>();
      for (JsonNode entry : node) {
        values.add(entry.asText());
      }
      return String.join(",", values);
    }
    return node.asText();
  }

  @Override
  public double getNumericSetting(String key) {
    JsonNode node = resolve(key);
    if (node == null || node.isMissingNode()) {
      throw new IllegalArgumentException("No numeric setting found for key: " + key);
    }
    return node.asDouble();
  }

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

  private List<ComponentInfo> parseComponents(JsonNode array) {
    List<ComponentInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      result.add(parseComponent(entry));
    }
    return result;
  }

  private ComponentInfo parseComponent(JsonNode entry) {
    String id = entry.path("id").asText("");
    String spriteUrl = entry.path("spriteUrl").asText("");
    double mass = entry.path("mass").asDouble(0);
    List<ButtonInfo> buttons = parseButtons(entry.path("buttons"));
    return new ComponentInfo(id, spriteUrl, mass, buttons);
  }

  private List<ButtonInfo> parseButtons(JsonNode array) {
    List<ButtonInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      String name = entry.path("name").asText("");
      double dx = entry.path("direction").path("x").asDouble(0);
      double dy = entry.path("direction").path("y").asDouble(0);
      result.add(new ButtonInfo(name, dx, dy));
    }
    return result;
  }

  private List<StaticComponentInfo> parseStaticComponents(JsonNode array) {
    List<StaticComponentInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      result.add(parseStaticComponent(entry));
    }
    return result;
  }

  private StaticComponentInfo parseStaticComponent(JsonNode entry) {
    String id = entry.path("id").asText("");
    String spriteUrl = entry.path("spriteUrl").asText("");
    double mass = entry.path("mass").asDouble(0);
    CollisionAction action = parseCollisionAction(entry.path("onCollisionAction"));
    return new StaticComponentInfo(id, spriteUrl, mass, action);
  }

  private CollisionAction parseCollisionAction(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    String type = node.path("type").asText("");
    double amount = node.path("amount").asDouble(0);
    return new CollisionAction(type, amount);
  }

  private List<TerrainInfo> parseTerrainTypes(JsonNode array) {
    List<TerrainInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      String id = entry.path("id").asText("");
      String spriteUrl =
          entry.path("spriteUrl").isNull() ? null : entry.path("spriteUrl").asText(null);
      String visualColor =
          entry.path("visualColor").isNull() ? null : entry.path("visualColor").asText(null);
      double friction = entry.path("friction").asDouble(0);
      result.add(new TerrainInfo(id, spriteUrl, visualColor, friction));
    }
    return result;
  }

  private List<CheatInfo> parseCheats(JsonNode array) {
    List<CheatInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      result.add(parseCheat(entry));
    }
    return result;
  }

  private CheatInfo parseCheat(JsonNode entry) {
    return new CheatInfo(
        entry.path("id").asText(""),
        entry.path("name").asText(""),
        entry.path("enabled").asBoolean(true),
        parseCheatTrigger(entry.path("trigger")),
        parseCheatEffects(entry.path("effects")));
  }

  private CheatTriggerInfo parseCheatTrigger(JsonNode node) {
    List<String> keys = new ArrayList<>();
    JsonNode keyNodes = node.path("keys");
    if (keyNodes.isArray()) {
      for (JsonNode key : keyNodes) {
        keys.add(key.asText(""));
      }
    }
    return new CheatTriggerInfo(keys);
  }

  private List<CheatEffectInfo> parseCheatEffects(JsonNode array) {
    List<CheatEffectInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      result.add(parseCheatEffect(entry));
    }
    return result;
  }

  private CheatEffectInfo parseCheatEffect(JsonNode entry) {
    return new CheatEffectInfo(
        entry.path("type").asText(""),
        parseCheatTarget(entry.path("target")),
        parseCheatParameters(entry.path("parameters")));
  }

  private CheatTargetInfo parseCheatTarget(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    return new CheatTargetInfo(node.path("kind").asText(""), node.path("id").asText(""));
  }

  private Map<String, String> parseCheatParameters(JsonNode node) {
    Map<String, String> result = new LinkedHashMap<>();
    if (node.isObject()) {
      node.fields().forEachRemaining(field -> result.put(field.getKey(), field.getValue().asText()));
    }
    return result;
  }
}
