package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PositionConstraint;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests build-rule lookup for default and catalog-backed vehicle components.
 *
 * @author Emilio Garcia
 */
class DefaultBuildRulesRegistryTest {

  @Test
  void Lookup_BigPigCatalogComponentRegistered_RequiresFrameContainment() {
    DefaultBuildRulesRegistry registry = new DefaultBuildRulesRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(Containment.INSIDE_FRAME, registry.lookup("BigPig").containment());
  }

  @Test
  void Lookup_WoodWheelDefaultComponentRequiresBottomRow_ReturnsBottomRowConstraint() {
    DefaultBuildRulesRegistry registry = new DefaultBuildRulesRegistry();

    assertEquals(PositionConstraint.BOTTOM_ROW_OF_GRID, registry.lookup("Wood Wheel").position());
  }

  @Test
  void Lookup_UnknownComponent_ThrowsIllegalArgumentException() {
    DefaultBuildRulesRegistry registry = new DefaultBuildRulesRegistry();

    assertThrows(IllegalArgumentException.class, () -> registry.lookup("MissingPart"));
  }
}
