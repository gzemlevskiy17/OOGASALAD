package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests full map-section normalization.
 *
 * @author Emilio Garcia
 */
class GeneratedMapSpecNormalizerTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void Normalize_MapWithTooFewKnots_SynthesizesDefaultTerrainKnots() {
    GeneratedMapSpecNormalizer normalizer = new GeneratedMapSpecNormalizer(PROPS);
    GeneratedLevelSpec.MapSpec sparseMap = new GeneratedLevelSpec.MapSpec(
        List.of(new GeneratedLevelSpec.PointSpec(100.0, 100.0)),
        new GeneratedLevelSpec.PointSpec(300.0, 900.0),
        List.of(new GeneratedLevelSpec.MapObjectSpec(
            "GoalLine", new GeneratedLevelSpec.PointSpec(2600.0, 900.0))),
        List.of("GrassHills"));

    GeneratedLevelSpec.MapSpec map =
        normalizer.normalize(sparseMap, 3200.0, 1600.0, GeneratedLevelFixtures.catalog());

    assertTrue(map.knotLocations().size() >= 8);
  }

  @Test
  void Normalize_NullMap_SynthesizesPlayableMapDefaults() {
    GeneratedMapSpecNormalizer normalizer = new GeneratedMapSpecNormalizer(PROPS);

    GeneratedLevelSpec.MapSpec map =
        normalizer.normalize(null, 3200.0, 1600.0, GeneratedLevelFixtures.catalog());

    assertFalse(map.knotLocations().isEmpty());
    assertTrue(map.objects().stream().anyMatch(object -> "GoalLine".equals(object.type())));
    assertFalse(map.terrainOptions().isEmpty());
  }
}
