package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PositionConstraint;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests build-rule lookup for default and catalog-backed vehicle components.
 *
 * @author Emilio Garcia
 */
class DefaultBuildRulesRegistryTest {

  @TempDir
  Path tempDir;

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

  @Test
  void RegisterFromConfig_MainCharacterCatalogComponentHasButtons_RequiresFrameContainment()
      throws IOException {
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
