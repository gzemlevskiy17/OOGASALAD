package oogasalad.model.authoring.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelJsonBuildException;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.GameMetadataSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MainCharacterSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MapObjectSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MapSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PointSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts a normalized generated-level specification into the schema-aligned level JSON
 * structure used by the new Good Piggies authoring contract.
 *
 * <p>The emitted tree mirrors the new repository level shape:
 * <ul>
 *   <li>{@code gameMetadata}</li>
 *   <li>{@code map}</li>
 *   <li>{@code mainCharacter}</li>
 * </ul>
 *
 * <p>{@code cameraFollows} is written only when non-blank so the builder stays compatible with the
 * branch's sample level files while the checked-in schema definition catches up.
 * @author Emilio Garcia
 */
public class GeneratedLevelJsonBuilder {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelJsonBuilder.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelJsonBuilder");

  /**
   * Builds a schema-aligned level JSON tree from the provided normalized generated-level
   * specification.
   *
   * @param spec normalized generated-level specification
   * @return schema-aligned level JSON tree
   */
  public ObjectNode build(GeneratedLevelSpec spec) {
    if (spec == null) {
      String message = PROPS.getString("nullSpecError");
      LOG.error(message);
      throw new GeneratedLevelJsonBuildException(message);
    }

    try {
      ObjectNode root = MAPPER.createObjectNode();
      serializeGameMetadata(root.putObject(s("gameMetadataKey")), spec.gameMetadata());
      serializeMap(root.putObject(s("mapKey")), spec.map());
      serializeMainCharacter(root.putObject(s("mainCharacterKey")), spec.mainCharacter());
      return root;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("buildFailureError");
      LOG.error(message, exception);
      throw new GeneratedLevelJsonBuildException(message, exception);
    }
  }

  private void serializeGameMetadata(ObjectNode metadataNode, GameMetadataSpec metadata) {
    metadataNode.put(s("levelNameKey"), metadata.levelName());
    metadataNode.put(s("gameTypeKey"), metadata.gameType());
    metadataNode.put(s("screenWidthKey"), metadata.screenWidth());
    metadataNode.put(s("screenHeightKey"), metadata.screenHeight());
    metadataNode.put(s("gravityKey"), metadata.gravity());
    metadataNode.put(s("dragKey"), metadata.drag());
  }

  private void serializeMap(ObjectNode mapNode, MapSpec map) {
    writePoints(mapNode.putArray(s("knotLocationsKey")), map.knotLocations());
    writeMapObjects(mapNode.putArray(s("objectsKey")), map.objects());
    writeStrings(mapNode.putArray(s("terrainOptionsKey")), map.terrainOptions());
    writePoint(mapNode.putObject(s("spawnPointKey")), map.spawnPoint());
  }

  private void serializeMainCharacter(ObjectNode mainCharacterNode, MainCharacterSpec mainCharacter) {
    mainCharacterNode.put(s("gridSizeKey"), mainCharacter.gridSize());
    writeStrings(mainCharacterNode.putArray(s("elementsAllowedKey")), mainCharacter.elementsAllowed());
    mainCharacterNode.put(s("maxSelectableKey"), mainCharacter.maxSelectable());
    mainCharacterNode.put(s("maxHpKey"), mainCharacter.maxHP());
    if (mainCharacter.cameraFollows() != null && !mainCharacter.cameraFollows().isBlank()) {
      mainCharacterNode.put(s("cameraFollowsKey"), mainCharacter.cameraFollows());
    }
  }

  private void writeMapObjects(ArrayNode objectsNode, List<MapObjectSpec> objects) {
    for (MapObjectSpec object : objects) {
      ObjectNode objectNode = objectsNode.addObject();
      objectNode.put(s("typeKey"), object.type());
      writePoint(objectNode.putObject(s("positionKey")), object.position());
    }
  }

  private void writePoints(ArrayNode pointsNode, List<PointSpec> points) {
    for (PointSpec point : points) {
      writePoint(pointsNode.addObject(), point);
    }
  }

  private void writeStrings(ArrayNode valuesNode, List<String> values) {
    for (String value : values) {
      valuesNode.add(value);
    }
  }

  private void writePoint(ObjectNode pointNode, PointSpec point) {
    pointNode.put(s("xKey"), point.x());
    pointNode.put(s("yKey"), point.y());
  }

  private String s(String key) {
    return PROPS.getString(key);
  }
}
