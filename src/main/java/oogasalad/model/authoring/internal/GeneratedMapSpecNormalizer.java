package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes the schema {@code map} section for AI-generated Good Piggies levels.
 * @author Emilio Garcia
 */
final class GeneratedMapSpecNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedMapSpecNormalizer.class);

  private final NormalizationSupport support;
  private final GeneratedMapObjectNormalizer objectNormalizer;
  private final GeneratedMapSupportNormalizer supportNormalizer;

  /**
   * Creates a map normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   */
  GeneratedMapSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
    this.objectNormalizer = new GeneratedMapObjectNormalizer(props);
    this.supportNormalizer = new GeneratedMapSupportNormalizer(props);
  }

  /**
   * Returns a normalized schema {@code map} section.
   *
   * @param map generated map section
   * @param screenWidth normalized screen width
   * @param screenHeight normalized screen height
   * @param catalog fixed Good Piggies catalog
   * @return normalized map section
   */
  GeneratedLevelSpec.MapSpec normalize(GeneratedLevelSpec.MapSpec map, double screenWidth,
      double screenHeight, GoodPiggiesCatalog catalog) {
    GeneratedLevelSpec.MapSpec safeMap =
        map == null ? new GeneratedLevelSpec.MapSpec(List.of(), null, List.of(), List.of()) : map;
    GeneratedLevelSpec.PointSpec normalizedSpawnPoint =
        normalizeSpawnPoint(safeMap.spawnPoint(), screenWidth, screenHeight);
    List<GeneratedLevelSpec.MapObjectSpec> normalizedObjects =
        objectNormalizer.normalizeObjects(safeMap.objects(), screenWidth, screenHeight, catalog);
    List<GeneratedLevelSpec.PointSpec> normalizedKnots =
        normalizeKnots(safeMap.knotLocations(), screenWidth, screenHeight, normalizedSpawnPoint,
            normalizedObjects);
    return new GeneratedLevelSpec.MapSpec(
        normalizedKnots,
        supportNormalizer.adjustSpawnPointToGround(normalizedSpawnPoint, normalizedKnots),
        supportNormalizer.adjustObjectsToGround(normalizedObjects, normalizedKnots),
        objectNormalizer.normalizeTerrainOptions(safeMap.terrainOptions(), catalog));
  }

  private List<GeneratedLevelSpec.PointSpec> normalizeKnots(
      List<GeneratedLevelSpec.PointSpec> knotLocations, double screenWidth, double screenHeight,
      GeneratedLevelSpec.PointSpec spawnPoint, List<GeneratedLevelSpec.MapObjectSpec> objects) {
    List<GeneratedLevelSpec.PointSpec> safeKnots = support.safeList(knotLocations);
    int preferredKnotCount = support.n("preferredMinimumKnotCount");
    if (safeKnots.size() < preferredKnotCount) {
      LOG.warn(support.s("defaultKnotsSynthesizedLogMessage"), preferredKnotCount);
      safeKnots = synthesizeDefaultKnots(screenWidth, screenHeight);
    }

    List<GeneratedLevelSpec.PointSpec> normalized = new ArrayList<>();
    for (int index = 0; index < safeKnots.size(); index++) {
      normalized.add(normalizeKnotPoint(safeKnots.get(index), index, safeKnots.size(), screenWidth,
          screenHeight));
    }
    supportNormalizer.flattenSupportZones(normalized, spawnPoint, objects, screenWidth);
    return List.copyOf(normalized);
  }

  private GeneratedLevelSpec.PointSpec normalizeSpawnPoint(GeneratedLevelSpec.PointSpec spawnPoint,
      double screenWidth, double screenHeight) {
    return normalizePoint(
        spawnPoint,
        support.d("defaultSpawnX"),
        support.d("defaultSpawnY"),
        screenWidth,
        screenHeight);
  }

  private GeneratedLevelSpec.PointSpec normalizeKnotPoint(GeneratedLevelSpec.PointSpec point,
      int index, int totalCount, double screenWidth, double screenHeight) {
    GeneratedLevelSpec.PointSpec fallback = defaultKnotPoint(index, totalCount, screenWidth, screenHeight);
    return normalizePoint(point, fallback.x(), fallback.y(), screenWidth, screenHeight);
  }

  private GeneratedLevelSpec.PointSpec normalizePoint(GeneratedLevelSpec.PointSpec point,
      double defaultX, double defaultY, double screenWidth, double screenHeight) {
    double x = support.clamp(
        support.defaultIfNull(point == null ? null : point.x(), defaultX), 0.0, screenWidth);
    double y = support.clamp(
        support.defaultIfNull(point == null ? null : point.y(), defaultY), 0.0, screenHeight);
    return new GeneratedLevelSpec.PointSpec(x, y);
  }

  private List<GeneratedLevelSpec.PointSpec> synthesizeDefaultKnots(double screenWidth,
      double screenHeight) {
    List<Double> xRatios = support.csvDoubles("defaultKnotXRatios");
    List<Double> yRatios = support.csvDoubles("defaultKnotYRatios");
    int pointCount = Math.min(xRatios.size(), yRatios.size());
    List<GeneratedLevelSpec.PointSpec> knots = new ArrayList<>();
    for (int index = 0; index < pointCount; index++) {
      knots.add(new GeneratedLevelSpec.PointSpec(
          support.clamp(screenWidth * xRatios.get(index), 0.0, screenWidth),
          support.clamp(screenHeight * yRatios.get(index), 0.0, screenHeight)));
    }
    return List.copyOf(knots);
  }

  private GeneratedLevelSpec.PointSpec defaultKnotPoint(int index, int totalCount, double screenWidth,
      double screenHeight) {
    double normalizedIndex = totalCount <= 1 ? 0.0 : (double) index / (totalCount - 1);
    List<Double> yRatios = support.csvDoubles("defaultKnotYRatios");
    int nearestDefaultIndex = Math.min((int) Math.round(
        normalizedIndex * Math.max(yRatios.size() - 1, 0)), Math.max(yRatios.size() - 1, 0));
    return new GeneratedLevelSpec.PointSpec(
        screenWidth * normalizedIndex,
        support.clamp(screenHeight * yRatios.get(nearestDefaultIndex), 0.0, screenHeight));
  }

}
