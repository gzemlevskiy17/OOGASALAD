package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PositionConstraint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultBuildRulesRegistryTest {

  @TempDir
  Path tempDir;

  @Test
  void registerFromConfig_addsRulesForCatalogMainCharacter() throws IOException {
    DefaultBuildRulesRegistry registry = new DefaultBuildRulesRegistry();
    Path gameFile = tempDir.resolve("game.json");
    Files.writeString(gameFile, """
        {"gameId":"badpiggies","mainCharacterId":"BigPig",
         "components":[{"id":"BigPig","spriteUrl":"","mass":10,
           "buttons":[{"name":"Move Right","direction":{"x":100,"y":0}}]}],
         "staticComponents":[],"terrainTypes":[]}
        """);

    registry.registerFromConfig(new GameConfigImpl(gameFile.toString()));

    assertEquals(Containment.INSIDE_FRAME, registry.lookup("BigPig").containment());
    assertEquals(PositionConstraint.NONE, registry.lookup("BigPig").position());
  }
}
