package oogasalad.controller.player.internal.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.engine.internal.components.DriveComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VehiclePartRegistryTest {

  @TempDir
  Path tempDir;

  @Test
  void registerFromConfig_preservesBuiltInFanThrust() throws IOException {
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
