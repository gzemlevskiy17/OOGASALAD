package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;

/**
 * Applies playability shaping rules to normalized map terrain, spawn, and goal support zones.
 *
 * @author Emilio Garcia
 */
final class GeneratedMapSupportNormalizer {

  private static final String GOAL_LINE_TYPE = "GoalLine";

  private final NormalizationSupport support;

  /**
   * Creates a support-zone normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
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
   * @author Emilio Garcia
   */
  void flattenSupportZones(List<GeneratedLevelSpec.PointSpec> knots,
      GeneratedLevelSpec.PointSpec spawnPoint, List<GeneratedLevelSpec.MapObjectSpec> objects) {
    flattenSupportZone(knots, spawnPoint.x());
    findGoalLine(objects).ifPresent(goalLine -> flattenSupportZone(knots, goalLine.position().x()));
  }

  /**
   * Adjusts the spawn point upward when needed so it sits above the flattened support ground.
   *
   * @param spawnPoint normalized spawn point
   * @param knots support-shaped terrain knots
   * @return adjusted spawn point
   * @author Emilio Garcia
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
   * @author Emilio Garcia
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
