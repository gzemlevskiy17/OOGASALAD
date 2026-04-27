package oogasalad.model.authoring.internal;

import java.util.List;
import java.util.Set;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MapObjectSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.MapSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PointSpec;

/**
 * Validates the schema-native {@code map} section for generated Good Piggies levels.
 *
 * @author Emilio Garcia
 */
final class MapSpecValidator {

  private final Set<String> allowedStaticObjectIds;
  private final Set<String> allowedTerrainIds;
  private final ValidationSupport support;

  /**
   * @param allowedStaticObjectIds valid Good Piggies placed-object ids
   * @param allowedTerrainIds valid Good Piggies terrain option ids
   * @param support shared validation utilities and properties
   * @author Emilio Garcia
   */
  MapSpecValidator(Set<String> allowedStaticObjectIds, Set<String> allowedTerrainIds,
      ValidationSupport support) {
    this.allowedStaticObjectIds = allowedStaticObjectIds;
    this.allowedTerrainIds = allowedTerrainIds;
    this.support = support;
  }

  void validate(MapSpec map) {
    support.requirePresent("missingMapError", map);
    validateKnots(map.knotLocations());
    validatePoint(map.spawnPoint(), "spawnPointMissingError", "spawnCoordinateMissingError");
    validateObjects(map.objects());
    validateTerrainOptions(map.terrainOptions());
  }

  private void validateKnots(List<PointSpec> knots) {
    if (knots == null || knots.size() < support.n("minKnotCount")) {
      support.fail("knotCountInvalidError", support.n("minKnotCount"));
    }
    for (int i = 0; i < knots.size(); i++) {
      validatePoint(knots.get(i), "knotPointMissingError", "knotCoordinateMissingError", i);
    }
  }

  private void validateObjects(List<MapObjectSpec> objects) {
    if (objects == null) {
      support.fail("objectsMissingError");
    }
    boolean foundGoalLine = false;
    for (int i = 0; i < objects.size(); i++) {
      MapObjectSpec object = objects.get(i);
      if (object == null) {
        support.fail("objectEntryMissingError", i);
      }
      support.requireNonBlank("objectTypeMissingError", object.type(), i);
      if (!allowedStaticObjectIds.contains(object.type())) {
        support.fail("objectTypeInvalidError", object.type(), i);
      }
      validatePoint(object.position(), "objectPositionMissingError",
          "objectCoordinateMissingError", i);
      if ("GoalLine".equals(object.type())) {
        foundGoalLine = true;
      }
    }
    if (!foundGoalLine) {
      support.fail("goalLineMissingError");
    }
  }

  private void validateTerrainOptions(List<String> terrainOptions) {
    if (terrainOptions == null) {
      return;
    }
    for (int i = 0; i < terrainOptions.size(); i++) {
      String terrainId = terrainOptions.get(i);
      support.requireNonBlank("terrainOptionMissingError", terrainId, i);
      if (!allowedTerrainIds.contains(terrainId)) {
        support.fail("terrainOptionInvalidError", terrainId, i);
      }
    }
  }

  private void validatePoint(PointSpec point, String missingPointKey, String missingCoordinateKey,
      Object... args) {
    if (point == null) {
      support.fail(missingPointKey, args);
    }
    if (point.x() == null || point.y() == null) {
      support.fail(missingCoordinateKey, args);
    }
  }
}
