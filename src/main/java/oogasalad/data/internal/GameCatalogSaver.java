package oogasalad.data.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
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
    save(filePath, gameId, mainCharacterId, components, staticComponents, terrainTypes,
        List.of());
  }

  public void save(String filePath, String gameId, String mainCharacterId,
      List<ComponentInfo> components,
      List<StaticComponentInfo> staticComponents,
      List<TerrainInfo> terrainTypes,
      List<CheatInfo> cheats) {

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

    writeCheats(root, cheats);

    try {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
      LOG.info("Game catalog saved to {}", filePath);
    } catch (IOException e) {
      throw new LevelSaveException("Failed to write game catalog: " + filePath, e);
    }
  }

  private void writeCheats(ObjectNode root, List<CheatInfo> cheats) {
    ArrayNode cheatsNode = root.putArray("cheats");
    for (CheatInfo cheat : cheats) {
      ObjectNode cheatNode = cheatsNode.addObject();
      cheatNode.put("id", cheat.id());
      cheatNode.put("name", cheat.name());
      cheatNode.put("enabled", cheat.enabled());

      ArrayNode keysNode = cheatNode.putObject("trigger").putArray("keys");
      for (String key : cheat.trigger().keys()) {
        keysNode.add(key);
      }

      ArrayNode effectsNode = cheatNode.putArray("effects");
      for (CheatEffectInfo effect : cheat.effects()) {
        ObjectNode effectNode = effectsNode.addObject();
        effectNode.put("type", effect.type());
        writeCheatTarget(effectNode, effect.target());
        ObjectNode parametersNode = effectNode.putObject("parameters");
        effect.parameters().forEach(parametersNode::put);
      }
    }
  }

  private void writeCheatTarget(ObjectNode effectNode, CheatTargetInfo target) {
    if (target == null) {
      effectNode.putNull("target");
      return;
    }
    ObjectNode targetNode = effectNode.putObject("target");
    targetNode.put("kind", target.kind());
    targetNode.put("id", target.id());
  }
}
