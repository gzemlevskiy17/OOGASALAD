package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.PickupEffect;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.components.RockComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.effects.DamageEffect;
import oogasalad.model.engine.internal.effects.HealEffect;
import oogasalad.model.engine.internal.effects.ScoreEffect;
import oogasalad.model.player.api.ScoreService;
import oogasalad.model.player.internal.ScoreServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses a level JSON file and populates the given World with GameObjects.
 *
 * Expected JSON structure (see doc/plan/data/files/level1_flat_ground.json):
 * <pre>
 * {
 *   "level": {
 *     "terrain": [
 *       {
 *         "position": { "x": 0, "y": 0 },
 *         "size":     { "width": 60, "height": 2 },
 *         "properties": {
 *           "texture":      "assets/terrain/grass.png",
 *           "tile_texture": true,
 *           "friction":     0.6
 *         }
 *       }
 *     ]
 *   }
 * }
 * </pre>
 *
 * Throws LevelLoadException (unchecked) on missing file or invalid JSON.
 * Collaborators: WorldImpl (receives objects), TerrainBlock (created per terrain entry).
 */
public class LevelLoaderImpl implements LevelLoader {

  private static final Logger LOG = LogManager.getLogger(LevelLoaderImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final LevelValidator VALIDATOR = new LevelValidator();

  private static final double DEFAULT_FRICTION = 0.5;
  private static final double DEFAULT_SIZE = 1.0;
  private static final double DEFAULT_RESTITUTION = 0.5;
  private static final double DEFAULT_ROCK_RESTITUTION = 0.3;
  private static final double DEFAULT_ROCK_DAMAGE = 10.0;
  private static final double DEFAULT_PICKUP_AMOUNT = 10.0;

  private final ScoreService score = new ScoreServiceImpl();

  public ScoreService getScore() {
    return score;
  }

  @Override
  public void loadLevel(String fileName, World world, GameConfig config) {
    JsonNode root = parseFile(fileName);
    VALIDATOR.validate(root);

    loadTerrain(root.path("level").path("terrain"), world, fileName);
    loadRocks(root.path("level").path("rocks"), world);
    loadPickups(root.path("level").path("pickups"), world);
    loadFinishLine(root.path("level").path("finish_line"), world);
    LOG.info("Level loaded from {}", fileName);
  }

  private JsonNode parseFile(String fileName) {
    try {
      return MAPPER.readTree(new File(fileName));
    } catch (IOException e) {
      throw new LevelLoadException("Cannot read level file: " + fileName, e);
    }
  }

  private void loadTerrain(JsonNode terrainArray, World world, String sourceFile) {
    if (terrainArray.isMissingNode()) {
      LOG.warn("No 'terrain' array found in level file: {}", sourceFile);
      return;
    }
    for (JsonNode entry : terrainArray) {
      world.addObject(parseTerrain(entry));
    }
  }

  private void loadFinishLine(JsonNode node, World world) {
    if (node.isMissingNode()) {
      return;
    }
    double x = node.path("x").asDouble(0);
    double y = node.path("y").asDouble(0);
    double width = node.path("width").asDouble(20);
    double height = node.path("height").asDouble(400);
    world.addObject(new GoalLineComponent(x, y, width, height));
  }

  private void loadRocks(JsonNode rocksArray, World world) {
    if (rocksArray.isMissingNode()) {
      return;
    }
    for (JsonNode entry : rocksArray) {
      world.addObject(parseRock(entry));
    }
  }

  private RockComponent parseRock(JsonNode entry) {
    JsonNode pos = entry.path("position");
    JsonNode size = entry.path("size");
    JsonNode props = entry.path("properties");

    double x = pos.path("x").asDouble(0);
    double y = pos.path("y").asDouble(0);
    double width = size.path("width").asDouble(DEFAULT_SIZE);
    double height = size.path("height").asDouble(DEFAULT_SIZE);
    double friction = props.path("friction").asDouble(DEFAULT_FRICTION);
    double restitution = props.path("restitution").asDouble(DEFAULT_ROCK_RESTITUTION);
    double damage = props.path("damage").asDouble(DEFAULT_ROCK_DAMAGE);

    return new RockComponent(x, y, width, height, friction, restitution, damage);
  }

  private void loadPickups(JsonNode pickupsArray, World world) {
    if (pickupsArray.isMissingNode()) {
      return;
    }
    for (JsonNode entry : pickupsArray) {
      PickupComponent pickup = parsePickup(entry);
      pickup.setOnDestroyHook(() -> world.destroyObject(pickup));
      world.addObject(pickup);
    }
  }

  private PickupComponent parsePickup(JsonNode entry) {
    String type = entry.path("type").asText("");
    JsonNode pos = entry.path("position");
    JsonNode size = entry.path("size");
    JsonNode props = entry.path("properties");

    double x = pos.path("x").asDouble(0);
    double y = pos.path("y").asDouble(0);
    double width = size.path("width").asDouble(DEFAULT_SIZE);
    double height = size.path("height").asDouble(DEFAULT_SIZE);
    double amount = props.path("amount").asDouble(DEFAULT_PICKUP_AMOUNT);

    String typeName = pickupTypeName(type);
    PickupEffect effect = pickupEffectFor(type, amount);
    ConvexPolygon polygon = ConvexPolygon.axisAlignedBox(x - width / 2, y - height / 2, width,
        height);
    return new PickupComponent(x, y, 0, polygon, "", typeName, effect);
  }

  private String pickupTypeName(String type) {
    return switch (type) {
      case "heart" -> "Heart";
      case "bomb" -> "Bomb";
      case "coin" -> "Coin";
      case "xp" -> "XPBoost";
      default -> throw new LevelLoadException("Unknown pickup type: '" + type + "'");
    };
  }

  private PickupEffect pickupEffectFor(String type, double amount) {
    return switch (type) {
      case "heart" -> new HealEffect(amount);
      case "bomb" -> new DamageEffect(amount);
      case "coin", "xp" -> new ScoreEffect(score, (int) amount);
      default -> throw new LevelLoadException("Unknown pickup type: '" + type + "'");
    };
  }

  private TerrainBlock parseTerrain(JsonNode entry) {
    JsonNode pos = entry.path("position");
    JsonNode size = entry.path("size");
    JsonNode props = entry.path("properties");

    double x = pos.path("x").asDouble(0);
    double y = pos.path("y").asDouble(0);
    double width = size.path("width").asDouble(DEFAULT_SIZE);
    double height = size.path("height").asDouble(DEFAULT_SIZE);
    String texture = props.path("texture").asText("assets/terrain/default.png");
    boolean tiled = props.path("tile_texture").asBoolean(false);
    double friction = props.path("friction").asDouble(DEFAULT_FRICTION);
    double restitution = props.path("restitution").asDouble(DEFAULT_RESTITUTION);

    return new TerrainBlock(x, y, width, height, texture, tiled, friction, restitution);
  }
}
