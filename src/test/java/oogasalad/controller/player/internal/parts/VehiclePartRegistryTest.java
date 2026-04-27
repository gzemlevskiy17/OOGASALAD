package oogasalad.controller.player.internal.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests vehicle part registration for default and catalog-backed components.
 *
 * @author Emilio Garcia
 */
class VehiclePartRegistryTest {

  @Test
  void Get_BigPigCatalogComponentHasButtons_ReturnsPigJointStyle() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(JointStyle.PIG, registry.get("BigPig").jointStyle());
  }

  @Test
  void Get_FanCatalogComponentHasButtons_ReturnsStructuralDriveJointStyle() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(JointStyle.STRUCTURAL, registry.get("Fan").jointStyle());
  }

  @Test
  void Get_UnknownComponent_ThrowsIllegalArgumentException() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    assertThrows(IllegalArgumentException.class, () -> registry.get("MissingPart"));
  }
}
