package oogasalad.controller.player.internal.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests vehicle part registration for default and catalog-backed components.
 *
 * @author Emilio Garcia
 */
class VehiclePartRegistryTest {

  @TempDir
  Path tempDir;

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

  @Test
  void RegisterFromConfig_FanCatalogComponentMatchesBuiltIn_PreservesBuiltInFanThrust()
      throws IOException {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    Path gameFile = tempDir.resolve("game.json");
    Files.writeString(gameFile, """
        {"gameId":"badpiggies","mainCharacterId":"BigPig",
         "components":[
           {"id":"BigPig","spriteUrl":"","mass":10,"buttons":[]},
           {"id":"Fan","spriteUrl":"","mass":3,
            "buttons":[{"name":"Thrust Left","direction":{"x":-400,"y":0}}]}
         ],
         "staticComponents":[],"terrainTypes":[]}
        """);

    registry.registerFromConfig(new GameConfigImpl(gameFile.toString()));
    DriveComponent fan = (DriveComponent) registry.get("Fan").create(0, 0);

    assertEquals(10000.0, fan.getThrustForce(), 1e-6);
    assertEquals(1.0, fan.getThrustDirection().x(), 1e-6);
  }
}
