package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.ComponentInfo;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

class BuiltInVehicleCatalogTest {

  @Test
  void templateById_null_yieldsEmpty() {
    assertTrue(BuiltInVehicleCatalog.templateById(null).isEmpty());
  }

  @Test
  void templateById_blank_yieldsEmpty() {
    assertTrue(BuiltInVehicleCatalog.templateById("   ").isEmpty());
  }

  @Test
  void templateById_woodWheel_returnsWheelId() {
    assertEquals("Wood Wheel",
        BuiltInVehicleCatalog.templateById("Wood Wheel").orElseThrow().id());
  }

  @Test
  void templateById_unknown_yieldsEmpty() {
    assertTrue(BuiltInVehicleCatalog.templateById("NotAPart").isEmpty());
  }

  @Test
  void templateById_eachCallReturnsNewInstance() {
    assertTrue(
        BuiltInVehicleCatalog.templateById("Frame").orElseThrow()
            != BuiltInVehicleCatalog.templateById("Frame").orElseThrow());
  }

  @Test
  void idsNotYetInCatalog_null_listsEveryBuiltinId() {
    assertEquals(BuiltInVehicleCatalog.defaultComponentIds(),
        BuiltInVehicleCatalog.idsNotYetInCatalog(null));
  }

  @Test
  void idsNotYetInCatalog_omitsIdsAlreadyInCatalog() {
    assertFalse(BuiltInVehicleCatalog.idsNotYetInCatalog(List.of(
        new ComponentInfo("Frame", "", 1.0, List.of()))).contains("Frame"));
  }

  @Test
  void idsNotYetInCatalog_keepsCanonicalOrderOfRemaining() {
    assertEquals("Balloon", BuiltInVehicleCatalog.idsNotYetInCatalog(List.of(
        new ComponentInfo("BigPig", "", 1.0, List.of()),
        new ComponentInfo("SmallPig", "", 1.0, List.of()))).get(0));
  }

  @Test
  void defaultComponentIds_containsWoodWheel() {
    assertTrue(BuiltInVehicleCatalog.defaultComponentIds().contains("Wood Wheel"));
  }

  @Test
  void componentIdsFromConfig_null_isEmpty() {
    assertEquals(List.of(), BuiltInVehicleCatalog.componentIdsFromConfig(null));
  }

  @Test
  void componentIdsFromConfig_returnsDistinctTrimmedIds() {
    assertEquals(List.of("A", "B"),
        BuiltInVehicleCatalog.componentIdsFromConfig(new TestGameConfig("g", "A",
            List.of(
                new ComponentInfo("A", "", 0, List.of()),
                new ComponentInfo(" B ", "", 0, List.of()),
                new ComponentInfo("A", "", 0, List.of())),
            List.of(), List.of())));
  }
}