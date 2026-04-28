package oogasalad.model.authoring.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests generated-level specification record defaults and immutability.
 *
 * @author Emilio Garcia
 */
class GeneratedLevelSpecTest {

  @Test
  void Constructor_NullSections_ReplacesSectionsWithEmptyDefaults() {
    GeneratedLevelSpec spec = new GeneratedLevelSpec(null, null, null);

    assertEquals(List.of(), spec.map().knotLocations());
    assertEquals(List.of(), spec.mainCharacter().elementsAllowed());
  }

  @Test
  void MapSpec_MutableInputLists_CopiesListsDefensively() {
    List<GeneratedLevelSpec.PointSpec> mutableKnots = new ArrayList<>();
    mutableKnots.add(new GeneratedLevelSpec.PointSpec(1.0, 2.0));

    GeneratedLevelSpec.MapSpec map =
        new GeneratedLevelSpec.MapSpec(mutableKnots, null, List.of(), List.of());
    mutableKnots.add(new GeneratedLevelSpec.PointSpec(3.0, 4.0));

    assertEquals(1, map.knotLocations().size());
  }

  @Test
  void MainCharacterSpec_ElementsAllowedListMutation_DoesNotChangeRecordState() {
    List<String> mutableComponents = new ArrayList<>(List.of("Frame"));

    GeneratedLevelSpec.MainCharacterSpec mainCharacter =
        new GeneratedLevelSpec.MainCharacterSpec(60.0, mutableComponents, 5, 100.0, "vehicle");
    mutableComponents.add("BigPig");

    assertEquals(List.of("Frame"), mainCharacter.elementsAllowed());
  }

  @Test
  void MapSpec_CopiedKnotListMutation_ThrowsUnsupportedOperationException() {
    GeneratedLevelSpec.MapSpec map = new GeneratedLevelSpec.MapSpec(
        List.of(new GeneratedLevelSpec.PointSpec(1.0, 2.0)), null, List.of(), List.of());

    assertThrows(UnsupportedOperationException.class,
        () -> map.knotLocations().add(new GeneratedLevelSpec.PointSpec(3.0, 4.0)));
  }
}
