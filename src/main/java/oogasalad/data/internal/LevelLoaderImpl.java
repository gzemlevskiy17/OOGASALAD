package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
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
 * <p>A positive {@code scale} factor divides every position and size on read,
 * letting the authoring environment consume JSON written in world-pixel units
 * while still storing placements in grid-cell units internally. The no-arg
 * constructor uses {@code scale = 1}, which is the pass-through behavior the
 * gameplay runtime and existing tests rely on.
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
  private static final double DEFAULT_SCALE = 1.0;
  private static final int DEFAULT_SPLINE_RESOLUTION = 8;
  private static final double DEFAULT_SPAWN_X = 350.0;
  private static final double DEFAULT_SPAWN_Y = 300.0;
  private static final int DEFAULT_MAX_SELECTABLE = 5;

  private final double scale;

  public LevelLoaderImpl() {
    this(DEFAULT_SCALE);
  }

  /**
   * @param scale divisor applied to every terrain position and size on read;
   *              must be positive. Used by the authoring environment to
   *              convert incoming world-pixel values back into grid-cell units.
   */
  public LevelLoaderImpl(double scale) {
    if (scale <= 0) {
      throw new IllegalArgumentException("scale must be positive, got " + scale);
    }
    this.scale = scale;
  }

  @Override
  public void loadLevel(String fileName, World world, LevelAttributes levelAttributes,
      GameConfig config) {
    JsonNode root = parseFile(fileName);
    VALIDATOR.validate(root);

    JsonNode levelNode = root.path("level");
    loadTerrain(levelNode.path("terrain"), world, fileName);
    loadSplines(levelNode.path("splines"), world);
    loadFinishLine(levelNode.path("finish_line"), world);
    loadSpawnPoint(levelNode.path("spawn"), world);
    if (levelAttributes != null) {
      loadVehiclePalette(levelNode.path("vehicle_palette"), levelAttributes);
    }
    LOG.info("Level loaded from {}", fileName);
  }

  private void loadSplines(JsonNode splineArray, World world) {
    if (splineArray.isMissingNode()) {
      return;
    }
    for (JsonNode entry : splineArray) {
      world.addObject(parseSpline(entry));
    }
  }

  private TerrainSpline parseSpline(JsonNode entry) {
    List<Vector> knots = readPointList(entry.path("knots"));
    if (knots.size() < 2) {
      throw new LevelLoadException("spline entry must contain at least 2 knots");
    }
    List<Vector> controls = readPointList(entry.path("controls"));
    double baselineY = entry.path("baselineY").asDouble() / scale;
    int resolution = entry.path("resolution").asInt(DEFAULT_SPLINE_RESOLUTION);

    JsonNode props = entry.path("properties");
    String texture = props.path("texture").asText("");
    boolean tiled = props.path("tile_texture").asBoolean(false);
    double friction = props.path("friction").asDouble(DEFAULT_FRICTION);
    String color = props.path("color").asText("#4A2C0F");

    List<Vector> resolvedControls = controls.size() == knots.size() - 1 ? controls : null;
    ParabolaSpline spline = new ParabolaSpline(knots, resolvedControls, baselineY, resolution);
    return new TerrainSpline(spline, texture, tiled, friction, color);
  }

  private List<Vector> readPointList(JsonNode arrayNode) {
    List<Vector> points = new ArrayList<>();
    if (arrayNode.isMissingNode() || !arrayNode.isArray()) {
      return points;
    }
    for (JsonNode p : arrayNode) {
      double x = p.path("x").asDouble() / scale;
      double y = p.path("y").asDouble() / scale;
      points.add(new Vector(x, y));
    }
    return points;
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

  private void loadSpawnPoint(JsonNode node, World world) {
    double x = node.isMissingNode() ? DEFAULT_SPAWN_X : node.path("x").asDouble(DEFAULT_SPAWN_X) / scale;
    double y = node.isMissingNode() ? DEFAULT_SPAWN_Y : node.path("y").asDouble(DEFAULT_SPAWN_Y) / scale;
    world.addObject(new SpawnPointComponent(x, y));
  }

  private void loadFinishLine(JsonNode node, World world) {
    if (node.isMissingNode()) {
      return;
    }
    double x = node.path("x").asDouble(0) / scale;
    double y = node.path("y").asDouble(0) / scale;
    double width = node.path("width").asDouble(20) / scale;
    double height = node.path("height").asDouble(400) / scale;
    world.addObject(new GoalLineComponent(x, y, width, height));
  }

  private TerrainBlock parseTerrain(JsonNode entry) {
    JsonNode pos = entry.path("position");
    JsonNode size = entry.path("size");
    JsonNode props = entry.path("properties");

    double x = pos.path("x").asDouble(0) / scale;
    double y = pos.path("y").asDouble(0) / scale;
    double width = size.path("width").asDouble(DEFAULT_SIZE) / scale;
    double height = size.path("height").asDouble(DEFAULT_SIZE) / scale;
    String texture = props.path("texture").asText("assets/terrain/default.png");
    boolean tiled = props.path("tile_texture").asBoolean(false);
    double friction = props.path("friction").asDouble(DEFAULT_FRICTION);

    return new TerrainBlock(x, y, width, height, texture, tiled, friction);
  }

  private void loadVehiclePalette(JsonNode vehiclePaletteNode, LevelAttributes levelAttributes) {
    if (vehiclePaletteNode.isMissingNode()) {
      levelAttributes.setMaxSelectableVehicleItems(DEFAULT_MAX_SELECTABLE);
      levelAttributes.setAllowedVehicleComponents(java.util.List.of());
      return;
    }

    levelAttributes.setMaxSelectableVehicleItems(
        vehiclePaletteNode.path("max_selectable").asInt(DEFAULT_MAX_SELECTABLE));

    java.util.List<String> allowedComponents = new java.util.ArrayList<>();
    JsonNode allowedComponentsNode = vehiclePaletteNode.path("allowed_components");
    if (allowedComponentsNode.isArray()) {
      for (JsonNode componentNode : allowedComponentsNode) {
        allowedComponents.add(componentNode.asText());
      }
    }
    levelAttributes.setAllowedVehicleComponents(allowedComponents);
  }
}
