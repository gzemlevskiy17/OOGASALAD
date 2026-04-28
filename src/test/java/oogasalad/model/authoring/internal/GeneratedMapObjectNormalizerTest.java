package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests normalization of generated map objects and terrain options.
 *
 * @author Emilio Garcia
 */
class GeneratedMapObjectNormalizerTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void NormalizeObjects_NoGoalLineInGeneratedObjects_SynthesizesGoalLine() {
    GeneratedMapObjectNormalizer normalizer = new GeneratedMapObjectNormalizer(PROPS);

    List<GeneratedLevelSpec.MapObjectSpec> objects = normalizer.normalizeObjects(
        List.of(new GeneratedLevelSpec.MapObjectSpec(
            "Coin", new GeneratedLevelSpec.PointSpec(100.0, 100.0))),
        3200.0,
        1600.0,
        GeneratedLevelFixtures.catalog());

    assertTrue(objects.stream().anyMatch(object -> "GoalLine".equals(object.type())));
  }

  @Test
  void NormalizeObjects_InvalidObjectType_DropsUnsupportedObject() {
    GeneratedMapObjectNormalizer normalizer = new GeneratedMapObjectNormalizer(PROPS);

    List<GeneratedLevelSpec.MapObjectSpec> objects = normalizer.normalizeObjects(
        List.of(
            new GeneratedLevelSpec.MapObjectSpec(
                "MissingObject", new GeneratedLevelSpec.PointSpec(100.0, 100.0)),
            new GeneratedLevelSpec.MapObjectSpec(
                "GoalLine", new GeneratedLevelSpec.PointSpec(3000.0, 900.0))),
        3200.0,
        1600.0,
        GeneratedLevelFixtures.catalog());

    assertTrue(objects.stream().noneMatch(object -> "MissingObject".equals(object.type())));
  }

  @Test
  void NormalizeTerrainOptions_InvalidTerrainOptions_UsesFirstCatalogTerrain() {
    GeneratedMapObjectNormalizer normalizer = new GeneratedMapObjectNormalizer(PROPS);

    List<String> terrainOptions = normalizer.normalizeTerrainOptions(
        List.of("MissingTerrain"), GeneratedLevelFixtures.catalog());

    assertEquals(List.of("GrassHills"), terrainOptions);
  }
}
