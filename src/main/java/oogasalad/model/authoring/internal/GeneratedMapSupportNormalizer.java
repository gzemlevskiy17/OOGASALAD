package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Applies playability shaping rules to normalized map terrain, spawn, and goal support zones.
 * @author Emilio Garcia
 */
final class GeneratedMapSupportNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedMapSupportNormalizer.class);
  private static final String GOAL_LINE_TYPE = "GoalLine";
  private static final double SAME_X_EPSILON = 1e-6;

  private final NormalizationSupport support;

  /**
   * Creates a support-zone normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   */
  GeneratedMapSupportNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Flattens short knot spans under the spawn point and goal line so both stand on stable ground.
   *
   * @param knots normalized terrain knots
   * @param spawnPoint normalized spawn point
   * @param objects normalized map objects
   */
  void flattenSupportZones(List<GeneratedLevelSpec.PointSpec> knots,
      GeneratedLevelSpec.PointSpec spawnPoint, List<GeneratedLevelSpec.MapObjectSpec> objects,
      double screenWidth) {
    ensureConnectedSupportZone(knots, spawnPoint.x(), screenWidth);
    flattenSupportZone(knots, spawnPoint.x());
    findGoalLine(objects).ifPresent(goalLine -> {
      ensureConnectedSupportZone(knots, goalLine.position().x(), screenWidth);
      flattenSupportZone(knots, goalLine.position().x());
    });
  }

  /**
   * Adjusts the spawn point upward when needed so it sits above the flattened support ground.
   *
   * @param spawnPoint normalized spawn point
   * @param knots support-shaped terrain knots
   * @return adjusted spawn point
   */
  GeneratedLevelSpec.PointSpec adjustSpawnPointToGround(
      GeneratedLevelSpec.PointSpec spawnPoint, List<GeneratedLevelSpec.PointSpec> knots) {
    double supportY = supportYForTarget(knots, spawnPoint.x());
    double minimumClearance = support.d("defaultSpawnGroundClearance");
    double maxAllowedY = supportY - minimumClearance;
    return spawnPoint.y() > maxAllowedY
        ? new GeneratedLevelSpec.PointSpec(spawnPoint.x(), maxAllowedY)
        : spawnPoint;
  }

  /**
   * Snaps goal-line objects onto the flattened support ground while leaving other objects alone.
   *
   * @param objects normalized map objects
   * @param knots support-shaped terrain knots
   * @return adjusted object list
   */
  List<GeneratedLevelSpec.MapObjectSpec> adjustObjectsToGround(
      List<GeneratedLevelSpec.MapObjectSpec> objects, List<GeneratedLevelSpec.PointSpec> knots) {
    List<GeneratedLevelSpec.MapObjectSpec> adjusted = new ArrayList<>();
    double goalLineClearance = support.d("defaultGoalLineGroundClearance");
    for (GeneratedLevelSpec.MapObjectSpec object : objects) {
      if (!GOAL_LINE_TYPE.equals(object.type())) {
        adjusted.add(object);
        continue;
      }
      double supportY = supportYForTarget(knots, object.position().x());
      adjusted.add(new GeneratedLevelSpec.MapObjectSpec(
          object.type(),
          new GeneratedLevelSpec.PointSpec(object.position().x(), supportY - goalLineClearance)));
    }
    return List.copyOf(adjusted);
  }

  private void flattenSupportZone(List<GeneratedLevelSpec.PointSpec> knots, double targetX) {
    if (knots.isEmpty()) {
      return;
    }
    int centerIndex = nearestKnotIndex(knots, targetX);
    int halfSpan = support.n("supportZoneHalfSpan");
    double flatY = knots.get(centerIndex).y();
    for (int index = Math.max(0, centerIndex - halfSpan);
         index <= Math.min(knots.size() - 1, centerIndex + halfSpan); index++) {
      GeneratedLevelSpec.PointSpec knot = knots.get(index);
      knots.set(index, new GeneratedLevelSpec.PointSpec(knot.x(), flatY));
    }
  }

  private void ensureConnectedSupportZone(List<GeneratedLevelSpec.PointSpec> knots, double targetX,
      double screenWidth) {
    if (knots.isEmpty()) {
      return;
    }
    int nearestIndex = nearestKnotIndex(knots, targetX);
    GeneratedLevelSpec.PointSpec nearest = knots.get(nearestIndex);
    double flatY = nearest.y();
    double gap = Math.abs(nearest.x() - targetX);
    double maximumSupportedGap = support.d("supportZoneMaxGap");
    double halfWidth = support.d("supportPlatformHalfWidth");

    if (gap > maximumSupportedGap) {
      addBridgeKnot(knots, nearest, targetX, flatY);
    }
    addSupportKnot(knots, support.clamp(targetX - halfWidth, 0.0, screenWidth), flatY);
    addSupportKnot(knots, support.clamp(targetX, 0.0, screenWidth), flatY);
    addSupportKnot(knots, support.clamp(targetX + halfWidth, 0.0, screenWidth), flatY);
    knots.sort(Comparator.comparingDouble(GeneratedLevelSpec.PointSpec::x));
    LOG.debug("Shaped generated terrain support around x={}", targetX);
  }

  private void addBridgeKnot(List<GeneratedLevelSpec.PointSpec> knots,
      GeneratedLevelSpec.PointSpec nearest, double targetX, double flatY) {
    double direction = targetX >= nearest.x() ? 1.0 : -1.0;
    double gap = Math.abs(nearest.x() - targetX);
    double bridgeStep = support.d("supportBridgeStep");
    double bridgeX = nearest.x() + direction * Math.min(bridgeStep, gap / 2.0);
    addSupportKnot(knots, bridgeX, flatY);
  }

  private void addSupportKnot(List<GeneratedLevelSpec.PointSpec> knots, double x, double y) {
    for (int index = 0; index < knots.size(); index++) {
      GeneratedLevelSpec.PointSpec knot = knots.get(index);
      if (Math.abs(knot.x() - x) <= SAME_X_EPSILON) {
        knots.set(index, new GeneratedLevelSpec.PointSpec(knot.x(), y));
        return;
      }
    }
    knots.add(new GeneratedLevelSpec.PointSpec(x, y));
  }

  private java.util.Optional<GeneratedLevelSpec.MapObjectSpec> findGoalLine(
      List<GeneratedLevelSpec.MapObjectSpec> objects) {
    return objects.stream()
        .filter(object -> GOAL_LINE_TYPE.equals(object.type()))
        .findFirst();
  }

  private double supportYForTarget(List<GeneratedLevelSpec.PointSpec> knots, double targetX) {
    return knots.get(nearestKnotIndex(knots, targetX)).y();
  }

  private int nearestKnotIndex(List<GeneratedLevelSpec.PointSpec> knots, double targetX) {
    int nearestIndex = 0;
    double nearestDistance = Math.abs(knots.get(0).x() - targetX);
    for (int index = 1; index < knots.size(); index++) {
      double distance = Math.abs(knots.get(index).x() - targetX);
      if (distance < nearestDistance) {
        nearestDistance = distance;
        nearestIndex = index;
      }
    }
    return nearestIndex;
  }
}
