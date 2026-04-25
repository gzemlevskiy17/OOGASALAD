package oogasalad.model.authoring.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelJsonBuildException;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.FinishLineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PickupSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PointSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.RockSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.SpawnSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainBlockSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainSplineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.VehiclePaletteSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts a normalized generated-level specification into the runtime JSON structure consumed by
 * the existing level loader.
 *
 * <p>The generated JSON mirrors the current repository schema while also writing optional metadata
 * fields for gravity and background image so authoring can preserve those values as the schema
 * evolves.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelJsonBuilder {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelJsonBuilder.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelJsonBuilder");

  /**
   * Builds a runtime level JSON tree from the provided normalized generated-level specification.
   *
   * @param spec normalized generated-level specification
   * @return runtime-compatible level JSON tree
   * @author Emilio Garcia
   */
  public ObjectNode build(GeneratedLevelSpec spec) {
    if (spec == null) {
      String message = PROPS.getString("nullSpecError");
      LOG.error(message);
      throw new GeneratedLevelJsonBuildException(message);
    }

    try {
      ObjectNode root = MAPPER.createObjectNode();
      ObjectNode level = root.putObject(s("levelKey"));

      serializeMetadata(level, spec);
      serializeTerrain(level.putArray(s("terrainKey")), spec);
      serializeSplines(level.putArray(s("splinesKey")), spec);
      serializeRocks(level.putArray(s("rocksKey")), spec);
      serializePickups(level.putArray(s("pickupsKey")), spec);
      serializeSpawn(level.putObject(s("spawnKey")), spec.spawn());
      serializeFinishLine(level.putObject(s("finishLineKey")), spec.finishLine());
      serializeVehiclePalette(level.putObject(s("vehiclePaletteKey")), spec.vehiclePalette());

      return root;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("buildFailureError");
      LOG.error(message, exception);
      throw new GeneratedLevelJsonBuildException(message, exception);
    }
  }

  private void serializeMetadata(ObjectNode level, GeneratedLevelSpec spec) {
    ObjectNode metadata = level.putObject(s("metadataKey"));
    metadata.put(s("levelNameKey"), spec.levelName());
    metadata.put(s("styleKey"), spec.style());
    metadata.put(s("gravityKey"), spec.gravity());
    if (spec.backgroundImage() != null && !spec.backgroundImage().isBlank()) {
      metadata.put(s("backgroundImageKey"), spec.backgroundImage());
    }
    if (spec.worldWidth() != null) {
      metadata.put(s("worldWidthKey"), spec.worldWidth());
    }
  }

  private void serializeTerrain(ArrayNode terrainArray, GeneratedLevelSpec spec) {
    for (TerrainBlockSpec block : spec.terrainBlocks()) {
      ObjectNode entry = terrainArray.addObject();
      writePosition(entry.putObject(s("positionKey")), block.x(), block.y());
      writeSize(entry.putObject(s("sizeKey")), block.width(), block.height());

      ObjectNode properties = entry.putObject(s("propertiesKey"));
      properties.put(s("textureKey"), block.texture());
      properties.put(s("tileTextureKey"), block.tileTexture());
      properties.put(s("frictionKey"), block.friction());
      properties.put(s("restitutionKey"), block.restitution());
    }
  }

  private void serializeSplines(ArrayNode splineArray, GeneratedLevelSpec spec) {
    for (TerrainSplineSpec spline : spec.terrainSplines()) {
      ObjectNode entry = splineArray.addObject();
      writePoints(entry.putArray(s("knotsKey")), spline.knots());
      writePoints(entry.putArray(s("controlsKey")), spline.controls());
      entry.put(s("baselineYKey"), spline.baselineY());
      entry.put(s("resolutionKey"), spline.resolution());

      ObjectNode properties = entry.putObject(s("propertiesKey"));
      properties.put(s("textureKey"), spline.texture());
      properties.put(s("tileTextureKey"), spline.tileTexture());
      properties.put(s("frictionKey"), spline.friction());
      properties.put(s("colorKey"), spline.color());
    }
  }

  private void serializeRocks(ArrayNode rocksArray, GeneratedLevelSpec spec) {
    for (RockSpec rock : spec.rocks()) {
      ObjectNode entry = rocksArray.addObject();
      writePosition(entry.putObject(s("positionKey")), rock.x(), rock.y());
      writeSize(entry.putObject(s("sizeKey")), rock.width(), rock.height());

      ObjectNode properties = entry.putObject(s("propertiesKey"));
      properties.put(s("frictionKey"), rock.friction());
      properties.put(s("restitutionKey"), rock.restitution());
      properties.put(s("damageKey"), rock.damage());
    }
  }

  private void serializePickups(ArrayNode pickupsArray, GeneratedLevelSpec spec) {
    for (PickupSpec pickup : spec.pickups()) {
      ObjectNode entry = pickupsArray.addObject();
      entry.put(s("typeKey"), pickup.type());
      writePosition(entry.putObject(s("positionKey")), pickup.x(), pickup.y());
      writeSize(entry.putObject(s("sizeKey")), pickup.width(), pickup.height());

      ObjectNode properties = entry.putObject(s("propertiesKey"));
      properties.put(s("amountKey"), pickup.amount());
    }
  }

  private void serializeSpawn(ObjectNode spawnNode, SpawnSpec spawn) {
    spawnNode.put(s("xKey"), spawn.x());
    spawnNode.put(s("yKey"), spawn.y());
  }

  private void serializeFinishLine(ObjectNode finishLineNode, FinishLineSpec finishLine) {
    finishLineNode.put(s("xKey"), finishLine.x());
    finishLineNode.put(s("yKey"), finishLine.y());
    finishLineNode.put(s("widthKey"), finishLine.width());
    finishLineNode.put(s("heightKey"), finishLine.height());
  }

  private void serializeVehiclePalette(ObjectNode paletteNode, VehiclePaletteSpec vehiclePalette) {
    paletteNode.put(s("maxSelectableKey"), vehiclePalette.maxSelectable());
    ArrayNode allowedComponents = paletteNode.putArray(s("allowedComponentsKey"));
    for (String componentId : vehiclePalette.allowedComponents()) {
      allowedComponents.add(componentId);
    }
  }

  private void writePosition(ObjectNode node, double x, double y) {
    node.put(s("xKey"), x);
    node.put(s("yKey"), y);
  }

  private void writeSize(ObjectNode node, double width, double height) {
    node.put(s("widthKey"), width);
    node.put(s("heightKey"), height);
  }

  private void writePoints(ArrayNode pointsArray, java.util.List<PointSpec> points) {
    for (PointSpec point : points) {
      ObjectNode entry = pointsArray.addObject();
      entry.put(s("xKey"), point.x());
      entry.put(s("yKey"), point.y());
    }
  }

  private String s(String key) {
    return PROPS.getString(key);
  }
}
