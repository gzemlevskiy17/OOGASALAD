package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.effects.HPChangerEffect;
import oogasalad.model.engine.internal.effects.ScoreEffect;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.model.player.api.ScoreService;
import oogasalad.model.player.internal.ScoreServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses a level.json file (new schema) and populates a {@link World} plus {@link LevelAttributes}.
 * The matching {@link GameConfig} is required so {@code mapObject.type} strings resolve against
 * the catalog of {@link StaticComponentInfo} entries.
 *
 * @author Paulina Vvedenskaya
 */
public class LevelLoaderImpl implements LevelLoader {

  private static final Logger LOG = LogManager.getLogger(LevelLoaderImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final LevelValidator VALIDATOR = new LevelValidator();

  private static final double DEFAULT_OBJECT_SIZE = 60;
  private static final double SPLINE_BASELINE_MARGIN = 200;

  private ScoreService score = new ScoreServiceImpl();

  public ScoreService getScore() {
    return score;
  }

  @Override
  public void loadLevel(String fileName, GameConfig game, World world,
      LevelAttributes levelAttributes) {
    score = new ScoreServiceImpl();
    JsonNode root = parseFile(fileName);
    LevelAttributes activeAttributes = levelAttributes;
    if (activeAttributes == null) {
      activeAttributes = new LevelAttributes();
    }

    populateGameMetadata(root.path("gameMetadata"), activeAttributes);
    populateMainCharacter(root.path("mainCharacter"), activeAttributes);
    validateGameTypeMatchesCatalog(activeAttributes, game);

    JsonNode map = root.path("map");
    loadTerrainSpline(map, game, world);
    loadBlocks(map.path("blocks"), world);
    loadSpawnPoint(map.path("spawnPoint"), world);
    loadObjects(map.path("objects"), game, world);

    LOG.info("Level loaded from {}", fileName);
  }

  private void validateGameTypeMatchesCatalog(LevelAttributes attrs, GameConfig game) {
    if (game == null || game.gameId() == null || game.gameId().isBlank()) {
      return;
    }
    if (attrs.getGameType().equals(game.gameId())) {
      return;
    }
    throw new LevelLoadException("Level gameType '" + attrs.getGameType()
        + "' does not match game catalog '" + game.gameId() + "'");
  }

  @Override
  public LevelAttributes loadAttributes(String fileName) {
    JsonNode root = parseFile(fileName);
    LevelAttributes attrs = new LevelAttributes();
    populateGameMetadata(root.path("gameMetadata"), attrs);
    populateMainCharacter(root.path("mainCharacter"), attrs);
    return attrs;
  }

  private JsonNode parseFile(String fileName) {
    JsonNode root;
    try {
      root = MAPPER.readTree(new File(fileName));
    } catch (IOException e) {
      throw new LevelLoadException("Could not read level file: " + fileName, e);
    }
    VALIDATOR.validate(root);
    return root;
  }

  private void populateGameMetadata(JsonNode node, LevelAttributes attrs) {
    if (node.isMissingNode()) {
      return;
    }
    attrs.setLevelName(node.path("levelName").asText(attrs.getLevelName()));
    attrs.setGameType(node.path("gameType").asText(attrs.getGameType()));
    attrs.setScreenWidth(node.path("screenWidth").asDouble(attrs.getScreenWidth()));
    attrs.setScreenHeight(node.path("screenHeight").asDouble(attrs.getScreenHeight()));
    attrs.setGravity(node.path("gravity").asDouble(attrs.getGravity()));
    attrs.setDrag(node.path("drag").asDouble(attrs.getDrag()));
  }

  private void populateMainCharacter(JsonNode node, LevelAttributes attrs) {
    if (node.isMissingNode()) {
      return;
    }
    attrs.setGridSize(node.path("gridSize").asDouble(attrs.getGridSize()));
    attrs.setMaxSelectable(node.path("maxSelectable").asInt(attrs.getMaxSelectable()));
    attrs.setMaxHP(node.path("maxHP").asDouble(attrs.getMaxHP()));
    attrs.setCameraFollows(node.path("cameraFollows").asText(attrs.getCameraFollows()));
    JsonNode allowed = node.path("elementsAllowed");
    if (allowed.isArray()) {
      List<String> ids = new ArrayList<>();
      for (JsonNode entry : allowed) {
        ids.add(entry.asText());
      }
      attrs.setMainCharacterElementsAllowed(ids);
    }
  }

  private static final String DEFAULT_TERRAIN_COLOR = "#4A2C0F";

  private void loadTerrainSpline(JsonNode map, GameConfig game, World world) {
    JsonNode knotsNode = map.path("knotLocations");
    if (!knotsNode.isArray() || knotsNode.size() < 2) {
      return;
    }
    List<Vector> knots = readPointList(knotsNode);
    double baselineY = baselineFor(knots);
    ParabolaSpline spline = new ParabolaSpline(knots, baselineY);
    TerrainInfo terrain = pickTerrain(map.path("terrainOptions"), game);
    world.addObject(buildTerrainSpline(spline, terrain));
  }

  private TerrainSpline buildTerrainSpline(ParabolaSpline spline, TerrainInfo terrain) {
    if (terrain == null) {
      return new TerrainSpline(spline, 0);
    }
    String sprite = terrain.spriteUrl();
    if (sprite == null) {
      sprite = "";
    }
    String color = terrain.visualColor();
    if (color == null) {
      color = DEFAULT_TERRAIN_COLOR;
    }
    return new TerrainSpline(terrain.id(), spline, sprite, true, terrain.friction(), color);
  }

  private TerrainInfo pickTerrain(JsonNode terrainOptions, GameConfig game) {
    if (game == null || !terrainOptions.isArray() || terrainOptions.size() == 0) {
      return null;
    }
    String firstId = terrainOptions.get(0).asText().trim();
    if (firstId.isEmpty()) {
      return null;
    }
    for (TerrainInfo info : game.terrainTypes()) {
      if (info.id().equals(firstId)) {
        return info;
      }
    }
    String lower = firstId.toLowerCase();
    for (TerrainInfo info : game.terrainTypes()) {
      if (info.id().toLowerCase().equals(lower)) {
        return info;
      }
    }
    LOG.warn("Unknown terrain option '{}'; loading spline without catalog terrain styling",
        firstId);
    return null;
  }

  private double baselineFor(List<Vector> knots) {
    double maxY = knots.get(0).y();
    for (Vector v : knots) {
      if (v.y() > maxY) {
        maxY = v.y();
      }
    }
    return maxY + SPLINE_BASELINE_MARGIN;
  }

  private List<Vector> readPointList(JsonNode arrayNode) {
    List<Vector> points = new ArrayList<>();
    for (JsonNode entry : arrayNode) {
      double x = entry.path("x").asDouble(0);
      double y = entry.path("y").asDouble(0);
      points.add(new Vector(x, y));
    }
    return points;
  }

  private void loadBlocks(JsonNode blocksNode, World world) {
    if (!blocksNode.isArray()) {
      return;
    }
    for (JsonNode entry : blocksNode) {
      String displayName = entry.path("displayName").asText("Terrain");
      double x = entry.path("x").asDouble(0);
      double y = entry.path("y").asDouble(0);
      double width = entry.path("width").asDouble(DEFAULT_OBJECT_SIZE);
      double height = entry.path("height").asDouble(DEFAULT_OBJECT_SIZE);
      String sprite = entry.path("sprite").asText("");
      boolean tiled = entry.path("tiled").asBoolean(true);
      double friction = entry.path("friction").asDouble(0.5);
      String color = entry.path("color").asText("#8B4513");
      world.addObject(new TerrainBlock(displayName, x, y, width, height, sprite, tiled, friction, color, 0.0));
    }
  }

  private void loadSpawnPoint(JsonNode spawnNode, World world) {
    if (spawnNode.isMissingNode()) {
      return;
    }
    double x = spawnNode.path("x").asDouble(0);
    double y = spawnNode.path("y").asDouble(0);
    world.addObject(new SpawnPointComponent(x, y));
  }

  private void loadObjects(JsonNode objectsArray, GameConfig game, World world) {
    if (!objectsArray.isArray()) {
      return;
    }
    if (game == null && objectsArray.size() > 0) {
      throw new LevelLoadException("Cannot load map.objects without a GameConfig");
    }
    for (JsonNode entry : objectsArray) {
      String typeId = entry.path("type").asText("").trim();
      double x = entry.path("position").path("x").asDouble(0);
      double y = entry.path("position").path("y").asDouble(0);
      StaticComponentInfo info = lookupStatic(typeId, game);
      if (info == null) {
        throw new LevelLoadException("Unknown map object type: '" + typeId + "'");
      }
      addStaticObject(info, x, y, world);
    }
  }

  private StaticComponentInfo lookupStatic(String typeId, GameConfig game) {
    if (game == null || typeId.isEmpty()) {
      return null;
    }
    for (StaticComponentInfo info : game.staticComponents()) {
      if (info.id().equals(typeId)) {
        return info;
      }
    }
    String lower = typeId.toLowerCase();
    for (StaticComponentInfo info : game.staticComponents()) {
      if (info.id().toLowerCase().equals(lower)) {
        return info;
      }
    }
    return null;
  }

  private void addStaticObject(StaticComponentInfo info, double x, double y, World world) {
    CollisionAction action = info.onCollisionAction();
    String actionType = null;
    if (action != null) {
      actionType = action.type();
    }

    if ("finish".equals(actionType)) {
      world.addObject(new GoalLineComponent(x, y, DEFAULT_OBJECT_SIZE, DEFAULT_OBJECT_SIZE));
      return;
    }

    CollisionEffect effect = effectFor(action);
    ConvexPolygon polygon = ConvexPolygon.axisAlignedBox(
        x - DEFAULT_OBJECT_SIZE / 2, y - DEFAULT_OBJECT_SIZE / 2,
        DEFAULT_OBJECT_SIZE, DEFAULT_OBJECT_SIZE);
    PickupComponent pickup = new PickupComponent(x, y, 0, polygon, "", info.id(), effect);
    pickup.setOnDestroyHook(() -> world.destroyObject(pickup));
    world.addObject(pickup);
  }

  private CollisionEffect effectFor(CollisionAction action) {
    if (action == null) {
      return target -> {
      };
    }
    String type = action.type();
    if ("heal".equals(type)) {
      return new HPChangerEffect(action.amount());
    }
    if ("damage".equals(type)) {
      return new HPChangerEffect(-action.amount());
    }
    if ("score".equals(type)) {
      return new ScoreEffect(score, (int) action.amount());
    }
    throw new LevelLoadException("Unknown collision action type: '" + type + "'");
  }
}
