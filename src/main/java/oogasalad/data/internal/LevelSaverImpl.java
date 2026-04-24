package oogasalad.data.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelSaveException;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serializes the current World state to a level JSON file.
 *
 * Mirrors the format read by LevelLoaderImpl so round-trip load → save → load
 * produces the same World. Only TerrainBlock objects are serialized; other
 * object types will be added as their implementations are introduced.
 *
 * Throws LevelSaveException (unchecked) on any I/O failure.
 * Collaborators: WorldImpl (source of objects), TerrainBlock (serialized type).
 */
public class LevelSaverImpl implements LevelSaver {

  private static final Logger LOG = LogManager.getLogger(LevelSaverImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public void saveLevel(String fileName, World world, LevelAttributes levelAttributes) {
    ObjectNode root = MAPPER.createObjectNode();
    ObjectNode level = root.putObject("level");
    ArrayNode terrain = level.putArray("terrain");

    for (GameObject obj : world.getObjects()) {
      if (obj instanceof TerrainBlock tb) {
        terrain.add(serializeTerrain(tb));
      }
    }

    serializeVehiclePalette(level, levelAttributes);

    try {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), root);
      LOG.info("Level saved to {}", fileName);
    } catch (IOException e) {
      throw new LevelSaveException("Failed to write level file: " + fileName, e);
    }
  }

  private ObjectNode serializeTerrain(TerrainBlock tb) {
    ObjectNode entry = MAPPER.createObjectNode();

    ObjectNode pos = entry.putObject("position");
    pos.put("x", tb.getX());
    pos.put("y", tb.getY());

    ObjectNode size = entry.putObject("size");
    size.put("width", tb.getWidth());
    size.put("height", tb.getHeight());

    ObjectNode props = entry.putObject("properties");
    props.put("texture", tb.getSprite());
    props.put("tile_texture", tb.isTiled());
    props.put("friction", tb.getFriction());

    return entry;
  }

  private void serializeVehiclePalette(ObjectNode level, LevelAttributes levelAttributes) {
    if (levelAttributes == null) {
      return;
    }

    ObjectNode vehiclePalette = level.putObject("vehicle_palette");
    vehiclePalette.put("max_selectable", levelAttributes.getMaxSelectableVehicleItems());
    ArrayNode allowedComponents = vehiclePalette.putArray("allowed_components");
    for (String componentId : levelAttributes.getAllowedVehicleComponents()) {
      allowedComponents.add(componentId);
    }
  }
}
