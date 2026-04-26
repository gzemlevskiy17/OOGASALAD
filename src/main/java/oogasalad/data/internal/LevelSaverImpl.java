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
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serializes the current World state to a level JSON file.
 *
 * Mirrors the format read by LevelLoaderImpl so round-trip load → save → load
 * produces the same World. Only TerrainBlock objects are serialized; other
 * object types will be added as their implementations are introduced.
 *
 * <p>A positive {@code scale} factor is applied to every position and size on
 * write, so the authoring environment (which stores terrain in grid-cell
 * units) can emit JSON in world-pixel units that the gameplay loader reads
 * verbatim. The no-arg constructor uses {@code scale = 1}, preserving the
 * pass-through behavior required by pre-existing level files and tests.
 *
 * Throws LevelSaveException (unchecked) on any I/O failure.
 * Collaborators: WorldImpl (source of objects), TerrainBlock (serialized type).
 */
public class LevelSaverImpl implements LevelSaver {

  private static final Logger LOG = LogManager.getLogger(LevelSaverImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final double DEFAULT_SCALE = 1.0;

  private final double scale;

  public LevelSaverImpl() {
    this(DEFAULT_SCALE);
  }

  /**
   * @param scale multiplier applied to every terrain position and size on
   *              write; must be positive. Used by the authoring environment
   *              to convert grid-cell units into world-pixel units.
   */
  public LevelSaverImpl(double scale) {
    if (scale <= 0) {
      throw new IllegalArgumentException("scale must be positive, got " + scale);
    }
    this.scale = scale;
  }

  @Override
  public void saveLevel(String fileName, World world, LevelAttributes levelAttributes) {
    ObjectNode root = MAPPER.createObjectNode();
    ObjectNode level = root.putObject("level");
    ArrayNode terrain = level.putArray("terrain");
    ArrayNode splines = level.putArray("splines");

    for (GameObject obj : world.getObjects()) {
      if (obj instanceof SpawnPointComponent sp) {
        ObjectNode spawn = level.putObject("spawn");
        spawn.put("x", sp.x() * scale);
        spawn.put("y", sp.y() * scale);
      } else if (obj instanceof TerrainSpline ts) {
        // Serialize splines as their source data only; the runtime re-tessellates on load so we
        // never have to write (or keep in sync) the derived slab polygons.
        splines.add(serializeSpline(ts));
      } else if (obj instanceof TerrainBlock tb) {
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

  private ObjectNode serializeSpline(TerrainSpline ts) {
    ObjectNode entry = MAPPER.createObjectNode();
    ParabolaSpline spline = ts.getSpline();

    ArrayNode knots = entry.putArray("knots");
    for (Vector k : spline.knots()) {
      ObjectNode point = knots.addObject();
      point.put("x", k.x() * scale);
      point.put("y", k.y() * scale);
    }

    ArrayNode controls = entry.putArray("controls");
    for (Vector c : spline.controls()) {
      ObjectNode point = controls.addObject();
      point.put("x", c.x() * scale);
      point.put("y", c.y() * scale);
    }

    entry.put("baselineY", spline.baselineY() * scale);

    ObjectNode props = entry.putObject("properties");
    props.put("texture", ts.getSprite());
    props.put("tile_texture", ts.isTiled());
    props.put("friction", ts.getFriction());
    props.put("color", ts.getColor());

    return entry;
  }

  private ObjectNode serializeTerrain(TerrainBlock tb) {
    ObjectNode entry = MAPPER.createObjectNode();

    ObjectNode pos = entry.putObject("position");
    pos.put("x", tb.x() * scale);
    pos.put("y", tb.y() * scale);

    ObjectNode size = entry.putObject("size");
    size.put("width", tb.getWidth() * scale);
    size.put("height", tb.getHeight() * scale);

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
    vehiclePalette.put("max_selectable", levelAttributes.getMainCharacter().getMaxSelectable());
    ArrayNode allowedComponents = vehiclePalette.putArray("allowed_components");
    for (String componentId : levelAttributes.getMainCharacter().getElementsAllowed()) {
      allowedComponents.add(componentId);
    }
  }
}
