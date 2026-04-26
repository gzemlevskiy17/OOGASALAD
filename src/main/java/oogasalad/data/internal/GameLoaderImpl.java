package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.Button;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.OnCollisionAction;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.api.Vector2;

/**
 * Reads a game.json file and produces a {@link GameConfig}. Each list in the catalog is parsed
 * top-down; missing optional fields default to sensible empty/null values rather than throwing.
 *
 * @author Paulina Vvedenskaya
 */
public class GameLoaderImpl implements GameLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public GameConfig loadGame(String fileName) {
    JsonNode root = parseFile(fileName);
    String gameId = requireText(root, "gameId");
    String primaryComponentId = requireText(root, "primaryComponentId");
    List<ComponentInfo> components = parseComponents(root.path("components"));
    List<StaticComponentInfo> staticComponents = parseStaticComponents(
        root.path("staticComponents"));
    List<TerrainInfo> terrainTypes = parseTerrainTypes(root.path("terrainTypes"));
    return new GameConfig(gameId, primaryComponentId, components, staticComponents, terrainTypes);
  }

  private JsonNode parseFile(String fileName) {
    try {
      return MAPPER.readTree(new File(fileName));
    } catch (IOException e) {
      throw new LevelLoadException("Could not read game file: " + fileName, e);
    }
  }

  private String requireText(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || !value.isTextual()) {
      throw new LevelLoadException("Missing or non-textual field: " + field);
    }
    return value.asText();
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
    String id = requireText(entry, "id");
    String spriteUrl = requireText(entry, "spriteUrl");
    double mass = entry.path("mass").asDouble(0);
    List<Button> buttons = parseButtons(entry.path("buttons"));
    return new ComponentInfo(id, spriteUrl, mass, buttons);
  }

  private List<Button> parseButtons(JsonNode array) {
    List<Button> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      String name = requireText(entry, "name");
      Vector2 direction = parseVector2(entry.path("direction"));
      result.add(new Button(name, direction));
    }
    return result;
  }

  private Vector2 parseVector2(JsonNode node) {
    double x = node.path("x").asDouble(0);
    double y = node.path("y").asDouble(0);
    return new Vector2(x, y);
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
    String id = requireText(entry, "id");
    String spriteUrl = requireText(entry, "spriteUrl");
    double mass = entry.path("mass").asDouble(0);
    OnCollisionAction action = parseOnCollisionAction(entry.path("onCollisionAction"));
    return new StaticComponentInfo(id, spriteUrl, mass, action);
  }

  private OnCollisionAction parseOnCollisionAction(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }
    String type = requireText(node, "type");
    JsonNode amountNode = node.path("amount");
    Double amount = amountNode.isMissingNode() || amountNode.isNull()
        ? null
        : amountNode.asDouble();
    return new OnCollisionAction(type, amount);
  }

  private List<TerrainInfo> parseTerrainTypes(JsonNode array) {
    List<TerrainInfo> result = new ArrayList<>();
    if (!array.isArray()) {
      return result;
    }
    for (JsonNode entry : array) {
      result.add(parseTerrain(entry));
    }
    return result;
  }

  private TerrainInfo parseTerrain(JsonNode entry) {
    String id = requireText(entry, "id");
    String spriteUrl = optionalText(entry, "spriteUrl");
    String visualColor = optionalText(entry, "visualColor");
    double friction = entry.path("friction").asDouble(0);
    return new TerrainInfo(id, spriteUrl, visualColor, friction);
  }

  private String optionalText(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asText();
  }
}
