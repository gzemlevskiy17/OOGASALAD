package oogasalad.model.authoring.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.ComponentInfo;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests Good Piggies catalog id projections and immutable list behavior.
 * @author Emilio Garcia
 */
class GoodPiggiesCatalogTest {

  @Test
  void ComponentIds_CatalogHasComponents_ReturnsOrderedComponentIds() {
    GoodPiggiesCatalog catalog = catalogWithComponents(List.of(
        TestGameConfig.component("BigPig"),
        TestGameConfig.component("Frame")));

    assertEquals(List.of("BigPig", "Frame"), catalog.componentIds());
  }

  @Test
  void StaticComponentIds_CatalogHasStaticComponents_ReturnsOrderedStaticIds() {
    GoodPiggiesCatalog catalog = catalogWithComponents(List.of(TestGameConfig.component("Frame")));

    assertEquals(List.of("GoalLine"), catalog.staticComponentIds());
  }

  @Test
  void Constructor_MutableComponentList_CopiesListDefensively() {
    List<ComponentInfo> mutableComponents = new ArrayList<>(List.of(
        TestGameConfig.component("Frame")));

    GoodPiggiesCatalog catalog = catalogWithComponents(mutableComponents);
    mutableComponents.add(TestGameConfig.component("BigPig"));

    assertEquals(List.of("Frame"), catalog.componentIds());
  }

  @Test
  void Constructor_NullComponentList_ThrowsNullPointerException() {
    assertThrows(NullPointerException.class,
        () -> new GoodPiggiesCatalog(
            "badpiggies",
            "BigPig",
            null,
            List.of(TestGameConfig.staticComponent("GoalLine")),
            List.of(TestGameConfig.terrain("GrassHills"))));
  }

  private GoodPiggiesCatalog catalogWithComponents(List<ComponentInfo> components) {
    return new GoodPiggiesCatalog(
        "badpiggies",
        "BigPig",
        components,
        List.of(TestGameConfig.staticComponent("GoalLine")),
        List.of(TestGameConfig.terrain("GrassHills")));
  }
}
