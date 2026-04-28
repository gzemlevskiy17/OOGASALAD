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
    validateRoot(root);
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

  private void validateRoot(JsonNode node) {
    requireObject(node, "game");
    requireText(node, "gameId", "gameId");
    requireText(node, "mainCharacterId", "mainCharacterId");
    requireArray(node, "components", "components");
    requireArray(node, "staticComponents", "staticComponents");
    requireArray(node, "terrainTypes", "terrainTypes");
    validateComponents(node.path("components"));
    validateStaticComponents(node.path("staticComponents"));
    validateTerrainTypes(node.path("terrainTypes"));
    validateMainCharacterReference(node);
  }

  private void validateMainCharacterReference(JsonNode node) {
    JsonNode componentsNode = node.path("components");
    if (componentsNode.size() == 0) {
      return;
    }
    String mainCharacter = node.path("mainCharacterId").asText();
    for (JsonNode component : componentsNode) {
      if (mainCharacter.equals(component.path("id").asText())) {
        return;
      }
    }
    throw new LevelLoadException("mainCharacterId must match an id in components");
  }

  private void validateComponents(JsonNode componentsNode) {
    for (int index = 0; index < componentsNode.size(); index++) {
      JsonNode component = componentsNode.get(index);
      String path = "components[" + index + "]";
      requireObject(component, path);
      requireText(component, "id", path + ".id");
      requireText(component, "spriteUrl", path + ".spriteUrl");
      requireNumber(component, "mass", path + ".mass");
      if (component.has("buttons")) {
        requireArray(component, "buttons", path + ".buttons");
        validateButtons(component.path("buttons"), path + ".buttons");
      }
    }
  }

  private void validateButtons(JsonNode buttonsNode, String path) {
    for (int index = 0; index < buttonsNode.size(); index++) {
      JsonNode button = buttonsNode.get(index);
      String buttonPath = path + "[" + index + "]";
      requireObject(button, buttonPath);
      requireText(button, "name", buttonPath + ".name");
      validatePoint(button.path("direction"), buttonPath + ".direction");
    }
  }

  private void validateStaticComponents(JsonNode staticComponentsNode) {
    for (int index = 0; index < staticComponentsNode.size(); index++) {
      JsonNode component = staticComponentsNode.get(index);
      String path = "staticComponents[" + index + "]";
      requireObject(component, path);
      requireText(component, "id", path + ".id");
      requireText(component, "spriteUrl", path + ".spriteUrl");
      requireNumber(component, "mass", path + ".mass");
      validateCollisionAction(component.path("onCollisionAction"), path + ".onCollisionAction");
    }
  }

  private void validateCollisionAction(JsonNode action, String path) {
    if (action.isMissingNode() || action.isNull()) {
      return;
    }
    requireObject(action, path);
    requireText(action, "type", path + ".type");
    if (action.has("amount")) {
      requireNumber(action, "amount", path + ".amount");
    }
  }

  private void validateTerrainTypes(JsonNode terrainTypesNode) {
    for (int index = 0; index < terrainTypesNode.size(); index++) {
      JsonNode terrain = terrainTypesNode.get(index);
      String path = "terrainTypes[" + index + "]";
      requireObject(terrain, path);
      requireText(terrain, "id", path + ".id");
      requireNumber(terrain, "friction", path + ".friction");
    }
  }

  private void validatePoint(JsonNode point, String path) {
    requireObject(point, path);
    requireNumber(point, "x", path + ".x");
    requireNumber(point, "y", path + ".y");
  }

  private void requireObject(JsonNode node, String path) {
    if (node == null || node.isMissingNode() || !node.isObject()) {
      throw new LevelLoadException("Game config missing required object: '" + path + "'");
    }
  }

  private void requireArray(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isArray()) {
      throw new LevelLoadException("Game config missing required array: '" + path + "'");
    }
  }

  private void requireText(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isTextual() || field.asText().isBlank()) {
      throw new LevelLoadException("Game config missing required string: '" + path + "'");
    }
  }

  private void requireNumber(JsonNode node, String key, String path) {
    JsonNode field = node.path(key);
    if (field.isMissingNode() || !field.isNumber()) {
      throw new LevelLoadException("Game config missing required number: '" + path + "'");
    }
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
      String spriteUrl = nullableText(entry.path("spriteUrl"));
      String visualColor = nullableText(entry.path("visualColor"));
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

  private String nullableText(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    return node.asText(null);
  }
}
