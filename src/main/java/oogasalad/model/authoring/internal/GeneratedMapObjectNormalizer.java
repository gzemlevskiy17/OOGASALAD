package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes schema map objects and terrain-option ids for AI-generated Good Piggies levels.
 *
 * @author Emilio Garcia
 */
final class GeneratedMapObjectNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedMapObjectNormalizer.class);
  private static final String GOAL_LINE_TYPE = "GoalLine";

  private final NormalizationSupport support;

  /**
   * Creates a map-object normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  GeneratedMapObjectNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns normalized placed map objects, ensuring at least one GoalLine exists.
   *
   * @param objects generated map objects
   * @param screenWidth normalized screen width
   * @param screenHeight normalized screen height
   * @param catalog fixed Good Piggies catalog
   * @return normalized map objects
   * @author Emilio Garcia
   */
  List<GeneratedLevelSpec.MapObjectSpec> normalizeObjects(
      List<GeneratedLevelSpec.MapObjectSpec> objects, double screenWidth, double screenHeight,
      GoodPiggiesCatalog catalog) {
    List<GeneratedLevelSpec.MapObjectSpec> normalized = new ArrayList<>();
    Set<String> allowedObjectIds = catalog.staticComponentIds();
    boolean foundGoalLine = false;

    for (GeneratedLevelSpec.MapObjectSpec object : support.safeList(objects)) {
      if (object == null) {
        continue;
      }
      String type = support.normalizeText(object.type(), "");
      if (type.isBlank()) {
        LOG.warn(support.s("blankObjectDroppedLogMessage"));
        continue;
      }
      if (!allowedObjectIds.contains(type)) {
        LOG.warn(support.s("invalidObjectDroppedLogMessage"), type);
        continue;
      }
      GeneratedLevelSpec.PointSpec defaultPosition = GOAL_LINE_TYPE.equals(type)
          ? defaultGoalLinePosition(screenWidth, screenHeight)
          : defaultObjectPosition(screenWidth, screenHeight);
      normalized.add(new GeneratedLevelSpec.MapObjectSpec(
          type,
          normalizePoint(object.position(), defaultPosition.x(), defaultPosition.y(), screenWidth,
              screenHeight)));
      if (GOAL_LINE_TYPE.equals(type)) {
        foundGoalLine = true;
      }
    }

    if (!foundGoalLine) {
      LOG.warn(support.s("goalLineSynthesizedLogMessage"));
      normalized.add(new GeneratedLevelSpec.MapObjectSpec(
          GOAL_LINE_TYPE, defaultGoalLinePosition(screenWidth, screenHeight)));
    }
    return List.copyOf(normalized);
  }

  /**
   * Returns normalized terrain option ids limited to the fixed Good Piggies catalog.
   *
   * @param terrainOptions generated terrain option ids
   * @param catalog fixed Good Piggies catalog
   * @return normalized terrain options
   * @author Emilio Garcia
   */
  List<String> normalizeTerrainOptions(List<String> terrainOptions, GoodPiggiesCatalog catalog) {
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String terrainId : support.safeList(terrainOptions)) {
      String candidate = support.normalizeText(terrainId, "");
      if (!candidate.isBlank() && catalog.terrainTypeIds().contains(candidate)) {
        normalized.add(candidate);
      }
    }
    if (normalized.isEmpty() && !catalog.terrainTypeIds().isEmpty()) {
      String fallbackTerrain = catalog.terrainTypeIds().iterator().next();
      LOG.warn(support.s("terrainOptionFallbackLogMessage"), fallbackTerrain);
      normalized.add(fallbackTerrain);
    }
    return List.copyOf(normalized);
  }

  private GeneratedLevelSpec.PointSpec normalizePoint(GeneratedLevelSpec.PointSpec point,
      double defaultX, double defaultY, double screenWidth, double screenHeight) {
    double x = support.clamp(
        support.defaultIfNull(point == null ? null : point.x(), defaultX), 0.0, screenWidth);
    double y = support.clamp(
        support.defaultIfNull(point == null ? null : point.y(), defaultY), 0.0, screenHeight);
    return new GeneratedLevelSpec.PointSpec(x, y);
  }

  private GeneratedLevelSpec.PointSpec defaultGoalLinePosition(double screenWidth,
      double screenHeight) {
    return new GeneratedLevelSpec.PointSpec(
        support.clamp(screenWidth - support.d("defaultGoalLineXOffset"), 0.0, screenWidth),
        support.clamp(screenHeight * support.d("defaultGoalLineYRatio"), 0.0, screenHeight));
  }

  private GeneratedLevelSpec.PointSpec defaultObjectPosition(double screenWidth,
      double screenHeight) {
    return new GeneratedLevelSpec.PointSpec(
        support.clamp(screenWidth * support.d("defaultObjectXRatio"), 0.0, screenWidth),
        support.clamp(screenHeight * support.d("defaultObjectYRatio"), 0.0, screenHeight));
  }
}
