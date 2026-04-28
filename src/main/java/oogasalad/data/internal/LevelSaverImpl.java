package oogasalad.data.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import oogasalad.data.api.LevelSaveException;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serializes the current authoring World state to the new level JSON schema.
 *
 * @author Paulina Vvedenskaya
 */
public class LevelSaverImpl implements LevelSaver {

  private static final Logger LOG = LogManager.getLogger(LevelSaverImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final double DEFAULT_SCALE = 1.0;

  private final double scale;

  public LevelSaverImpl() {
    this(DEFAULT_SCALE);
  }

  public LevelSaverImpl(double scale) {
    if (scale <= 0) {
      throw new IllegalArgumentException("scale must be positive, got " + scale);
    }
    this.scale = scale;
  }

  @Override
  public void saveLevel(String fileName, World world, LevelAttributes attrs) {
    ObjectNode root = MAPPER.createObjectNode();

    root.set("gameMetadata", buildGameMetadata(attrs));
    root.set("map", buildMap(world));
    root.set("mainCharacter", buildMainCharacter(attrs));

    try {
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), root);
      LOG.info("Level saved to {}", fileName);
    } catch (IOException e) {
      throw new LevelSaveException("Failed to write level file: " + fileName, e);
    }
  }

  private ObjectNode buildGameMetadata(LevelAttributes attrs) {
    ObjectNode meta = MAPPER.createObjectNode();
    meta.put("levelName", attrs.getLevelName());
    meta.put("gameType", attrs.getGameType());
    meta.put("screenWidth", attrs.getScreenWidth());
    meta.put("screenHeight", attrs.getScreenHeight());
    meta.put("gravity", attrs.getGravity());
    meta.put("drag", attrs.getDrag());
    return meta;
  }

  private ObjectNode buildMap(World world) {
    ObjectNode map = MAPPER.createObjectNode();

    ArrayNode knots = map.putArray("knotLocations");
    ArrayNode objects = map.putArray("objects");
    ArrayNode terrainOptions = map.putArray("terrainOptions");
    ObjectNode spawnPoint = map.putObject("spawnPoint");
    spawnPoint.put("x", 350.0);
    spawnPoint.put("y", 300.0);

    for (GameObject obj : world.getObjects()) {
      if (obj instanceof TerrainSpline ts) {
        serializeSplineKnots(ts, knots);
        terrainOptions.add(ts.getDisplayName());
      } else if (obj instanceof SpawnPointComponent sp) {
        spawnPoint.put("x", sp.x() * scale);
        spawnPoint.put("y", sp.y() * scale);
      } else if (obj instanceof AuthoringStaticComponent asc) {
        objects.add(buildMapObject(asc.getComponentType(),
            (asc.x() + asc.getWidth() / 2.0) * scale,
            (asc.y() + asc.getHeight() / 2.0) * scale));
      } else if (obj instanceof PickupComponent pc) {
        objects.add(
            buildMapObject(pc.getComponentType(), pc.x() * scale, pc.y() * scale));
      } else if (obj instanceof GoalLineComponent gl) {
        objects.add(buildMapObject("GoalLine", gl.x() * scale, gl.y() * scale));
      }
    }

    return map;
  }

  private void serializeSplineKnots(TerrainSpline ts, ArrayNode knots) {
    ParabolaSpline spline = ts.getSpline();
    for (Vector k : spline.knots()) {
      ObjectNode pt = knots.addObject();
      pt.put("x", k.x() * scale);
      pt.put("y", k.y() * scale);
    }
  }

  private ObjectNode buildMapObject(String type, double x, double y) {
    ObjectNode entry = MAPPER.createObjectNode();
    entry.put("type", type);
    ObjectNode pos = entry.putObject("position");
    pos.put("x", x);
    pos.put("y", y);
    return entry;
  }

  private ObjectNode buildMainCharacter(LevelAttributes attrs) {
    ObjectNode mc = MAPPER.createObjectNode();
    mc.put("gridSize", attrs.getGridSize());
    ArrayNode allowed = mc.putArray("elementsAllowed");
    for (String id : attrs.getMainCharacterElementsAllowed()) {
      allowed.add(id);
    }
    mc.put("maxSelectable", attrs.getMaxSelectable());
    mc.put("maxHP", attrs.getMaxHP());
    mc.put("cameraFollows", attrs.getCameraFollows());
    return mc;
  }
}
