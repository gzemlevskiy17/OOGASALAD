package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests terrain shaping rules for AI-generated levels.
 * @author Emilio Garcia
 */
class GeneratedMapSupportNormalizerTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void FlattenSupportZones_GoalNearExistingTerrain_AddsExactFlatSupportKnot() {
    GeneratedMapSupportNormalizer normalizer = new GeneratedMapSupportNormalizer(PROPS);
    List<GeneratedLevelSpec.PointSpec> knots = new ArrayList<>(List.of(
        new GeneratedLevelSpec.PointSpec(0.0, 800.0),
        new GeneratedLevelSpec.PointSpec(1000.0, 600.0),
        new GeneratedLevelSpec.PointSpec(2000.0, 800.0)));
    GeneratedLevelSpec.PointSpec spawn = new GeneratedLevelSpec.PointSpec(100.0, 700.0);
    GeneratedLevelSpec.MapObjectSpec goal = new GeneratedLevelSpec.MapObjectSpec(
        "GoalLine", new GeneratedLevelSpec.PointSpec(1800.0, 500.0));

    normalizer.flattenSupportZones(knots, spawn, List.of(goal), 2000.0);

    assertTrue(knots.stream().anyMatch(knot -> knot.x().equals(1800.0)));
  }

  @Test
  void AdjustObjectsToGround_GoalLineAboveSupportPlatform_SnapsBottomToSupportPlatform() {
    GeneratedMapSupportNormalizer normalizer = new GeneratedMapSupportNormalizer(PROPS);
    List<GeneratedLevelSpec.PointSpec> knots = new ArrayList<>(List.of(
        new GeneratedLevelSpec.PointSpec(0.0, 800.0),
        new GeneratedLevelSpec.PointSpec(1000.0, 600.0),
        new GeneratedLevelSpec.PointSpec(2000.0, 800.0)));
    GeneratedLevelSpec.PointSpec spawn = new GeneratedLevelSpec.PointSpec(100.0, 700.0);
    GeneratedLevelSpec.MapObjectSpec goal = new GeneratedLevelSpec.MapObjectSpec(
        "GoalLine", new GeneratedLevelSpec.PointSpec(1800.0, 500.0));

    normalizer.flattenSupportZones(knots, spawn, List.of(goal), 2000.0);
    List<GeneratedLevelSpec.MapObjectSpec> adjusted =
        normalizer.adjustObjectsToGround(List.of(goal), knots);

    assertEquals(770.0, adjusted.getFirst().position().y());
  }

  @Test
  void AdjustSpawnPointToGround_SpawnBelowMinimumClearance_MovesSpawnAboveTerrain() {
    GeneratedMapSupportNormalizer normalizer = new GeneratedMapSupportNormalizer(PROPS);
    List<GeneratedLevelSpec.PointSpec> knots = List.of(
        new GeneratedLevelSpec.PointSpec(0.0, 800.0),
        new GeneratedLevelSpec.PointSpec(1000.0, 800.0));
    GeneratedLevelSpec.PointSpec lowSpawn = new GeneratedLevelSpec.PointSpec(100.0, 780.0);

    GeneratedLevelSpec.PointSpec adjusted = normalizer.adjustSpawnPointToGround(lowSpawn, knots);

    assertEquals(480.0, adjusted.y());
  }
}
