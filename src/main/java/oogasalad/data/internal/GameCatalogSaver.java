package oogasalad.data.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.LevelSaveException;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serializes a game catalog to a {@code game.json} file following the game schema.
 */
public class GameCatalogSaver {

  private static final Logger LOG = LogManager.getLogger(GameCatalogSaver.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public void save(String filePath, String gameId, String mainCharacterId,
      List<ComponentInfo> components,
      List<StaticComponentInfo> staticComponents,
      List<TerrainInfo> terrainTypes) {

    ObjectNode root = MAPPER.createObjectNode();
    root.put("gameId", gameId);
    root.put("mainCharacterId", mainCharacterId);

    ArrayNode compsNode = root.putArray("components");
    for (ComponentInfo c : components) {
      ObjectNode n = compsNode.addObject();
      n.put("id", c.id());
      n.put("spriteUrl", c.spriteUrl());
      n.put("mass", c.mass());
      ArrayNode buttons = n.putArray("buttons");
      for (ButtonInfo b : c.buttons()) {
        ObjectNode bn = buttons.addObject();
        bn.put("name", b.name());
        ObjectNode dir = bn.putObject("direction");
        dir.put("x", b.directionX());
        dir.put("y", b.directionY());
      }
    }

    ArrayNode staticsNode = root.putArray("staticComponents");
    for (StaticComponentInfo s : staticComponents) {
      ObjectNode n = staticsNode.addObject();
      n.put("id", s.id());
      n.put("spriteUrl", s.spriteUrl());
      n.put("mass", s.mass());
      if (s.onCollisionAction() != null) {
        CollisionAction a = s.onCollisionAction();
        ObjectNode action = n.putObject("onCollisionAction");
        action.put("type", a.type());
        action.put("amount", a.amount());
      } else {
        n.putNull("onCollisionAction");
      }
    }

    ArrayNode terrainNode = root.putArray("terrainTypes");
    for (TerrainInfo t : terrainTypes) {
      ObjectNode n = terrainNode.addObject();
      n.put("id", t.id());
      if (t.spriteUrl() != null) {
        n.put("spriteUrl", t.spriteUrl());
      } else {
        n.putNull("spriteUrl");
      }
      if (t.visualColor() != null) {
        n.put("visualColor", t.visualColor());
      } else {
        n.putNull("visualColor");
      }
      n.put("friction", t.friction());
    }

    try {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
      LOG.info("Game catalog saved to {}", filePath);
    } catch (IOException e) {
      throw new LevelSaveException("Failed to write game catalog: " + filePath, e);
    }
  }
}
