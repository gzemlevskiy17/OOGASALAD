package oogasalad.controller.player.internal.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests vehicle part registration for default and catalog-backed components.
 *
 * @author Emilio Garcia
 * @author Paulina Vvedenskaya
 */
class VehiclePartRegistryTest {

  @Test
  void Get_BigPigCatalogComponentHasButtons_ReturnsPigJointStyle() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(JointStyle.PIG, registry.get("BigPig").jointStyle());
  }

  @Test
  void Get_FanCatalogComponentHasButtons_PreservesHardcodedSpec() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(JointStyle.STRUCTURAL, registry.get("Fan").jointStyle());
  }

  @Test
  void RegisterFromConfig_WheelWithNoButtons_PreservesWheelJointStyle() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());

    assertEquals(JointStyle.WHEEL, registry.get("Wood Wheel").jointStyle());
  }

  @Test
  void RegisterFromConfig_CustomComponentWithButtons_BecomesStructural() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Rocket", "", 5.0,
            List.of(new ButtonInfo("Boost", 0, -1000)))),
        List.of(), List.of());

    registry.registerFromConfig(config);
    VehiclePartSpec spec = registry.get("Rocket");

    assertEquals(JointStyle.STRUCTURAL, spec.jointStyle());
    assertInstanceOf(StructuralComponent.class, spec.create(0, 0));
  }

  @Test
  void RegisterFromConfig_CustomComponentWithNoButtons_BecomesStructural() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Plank", "", 3.0, List.of())),
        List.of(), List.of());

    registry.registerFromConfig(config);
    VehiclePartSpec spec = registry.get("Plank");

    assertEquals(JointStyle.STRUCTURAL, spec.jointStyle());
  }

  @Test
  void Get_UnknownComponent_ThrowsIllegalArgumentException() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    assertThrows(IllegalArgumentException.class, () -> registry.get("MissingPart"));
  }
}
