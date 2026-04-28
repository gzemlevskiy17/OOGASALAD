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

  @Test
  void Get_CaseInsensitiveLookup_FindsRegisteredPart() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    VehiclePartSpec spec = registry.get("frame");

    assertEquals("Frame", spec.paletteName());
  }

  @Test
  void Get_NormalizedLookup_FindsSpacedName() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    VehiclePartSpec spec = registry.get("woodwheel");

    assertEquals("Wood Wheel", spec.paletteName());
    assertEquals(JointStyle.WHEEL, spec.jointStyle());
  }

  @Test
  void RegisterFromConfig_NullConfig_DoesNotThrow() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    registry.registerFromConfig(null);

    assertEquals("Frame", registry.get("Frame").paletteName());
  }

  @Test
  void RegisterFromConfig_MainCharacterIdMatchesPigSpec() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "Hero",
        List.of(new ComponentInfo("Hero", "", 5.0, List.of())),
        List.of(), List.of());

    registry.registerFromConfig(config);

    assertEquals(JointStyle.PIG, registry.get("Hero").jointStyle(),
        "A component matching mainCharacterId should be registered as a pig spec.");
  }

  @Test
  void RegisterFromConfig_NameContainingPig_BecomesPigSpec() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("TinyPiglet", "", 3.0, List.of())),
        List.of(), List.of());

    registry.registerFromConfig(config);

    assertEquals(JointStyle.PIG, registry.get("TinyPiglet").jointStyle(),
        "A component whose name contains 'pig' should be a pig spec.");
  }

  @Test
  void Get_AllDefaultParts_RegisteredCorrectly() {
    VehiclePartRegistry registry = new VehiclePartRegistry();

    assertEquals(JointStyle.STRUCTURAL, registry.get("Frame").jointStyle());
    assertEquals(JointStyle.STRUCTURAL, registry.get("Sled").jointStyle());
    assertEquals(JointStyle.WHEEL, registry.get("Wood Wheel").jointStyle());
    assertEquals(JointStyle.WHEEL, registry.get("Metal Wheel").jointStyle());
    assertEquals(JointStyle.PIG, registry.get("Pig").jointStyle());
    assertEquals(JointStyle.PIG, registry.get("Small Pig").jointStyle());
    assertEquals(JointStyle.STRUCTURAL, registry.get("Engine").jointStyle());
    assertEquals(JointStyle.STRUCTURAL, registry.get("Fan").jointStyle());
    assertEquals(JointStyle.STRUCTURAL, registry.get("Balloon").jointStyle());
  }

  @Test
  void RegisterFromConfig_ExistingHardcodedPart_NotOverwritten() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Wood Wheel", "", 99.0, List.of())),
        List.of(), List.of());

    registry.registerFromConfig(config);

    assertEquals(JointStyle.WHEEL, registry.get("Wood Wheel").jointStyle(),
        "Hardcoded wheel spec should be preserved even if config has a matching id.");
  }

  @Test
  void RegisterFromConfig_PigOverridesExistingHardcodedPart() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    TestGameConfig config = new TestGameConfig("test", "Engine",
        List.of(new ComponentInfo("Engine", "", 5.0, List.of())),
        List.of(), List.of());

    registry.registerFromConfig(config);

    assertEquals(JointStyle.PIG, registry.get("Engine").jointStyle(),
        "If mainCharacterId matches a hardcoded part, pig should override it.");
  }
}
