package oogasalad.data.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoadException;
import oogasalad.data.api.LevelLoader;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
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
  private static final int DEFAULT_MAX_SELECTABLE = 5;

  @Override
  public void loadLevel(String fileName, World world, LevelAttributes levelAttributes,
      GameConfig config) {
    JsonNode root = parseFile(fileName);
    VALIDATOR.validate(root);

    JsonNode levelNode = root.path("level");
    loadTerrain(levelNode.path("terrain"), world, fileName);
    if (levelAttributes != null) {
      loadVehiclePalette(levelNode.path("vehicle_palette"), levelAttributes);
    }
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
