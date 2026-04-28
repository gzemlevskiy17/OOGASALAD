package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import org.junit.jupiter.api.Test;

/**
 * Tests authoring help text for cheat-key creation.
 *
 * @author Emilio Garcia
 */
class CheatAuthoringHelpDialogTest {

  @Test
  void ExampleText_DefaultRegistries_DisplaysMainCharacterPowerupExample() {
    String exampleText = new CheatAuthoringHelpDialog().exampleText();

    assertTrue(exampleText.contains("Spawn a heart powerup on the main character"),
        "Help text should show a complete cheat example.");
    assertTrue(exampleText.contains("Target Kind: mainCharacter"),
        "Example should teach target-based powerup spawning.");
    assertTrue(exampleText.contains("Parameters: componentId=heart"),
        "Example should avoid requiring coordinates for target-based spawning.");
  }

  @Test
  void AvailableText_CurrentCatalog_DisplaysRegistryAndCatalogOptions() {
    CheatAuthoringHelpDialog dialog = new CheatAuthoringHelpDialog(
        "BigPig",
        List.of(new ComponentInfo("Frame", "frame.png", 1.0, List.of())),
        List.of(new StaticComponentInfo("Heart", "heart.png", 0,
            new CollisionAction("heal", 25))),
        List.of(new TerrainInfo("Grass", "grass.png", null, 0.6)));

    String availableText = dialog.availableText();

    assertTrue(availableText.contains("- spawnPowerup"),
        "Help text should list registered effect types.");
    assertTrue(availableText.contains("- addScore"),
        "Help text should list score changes as an available primitive.");
    assertTrue(availableText.contains("- destroy"),
        "Help text should list destruction as an available primitive.");
    assertTrue(availableText.contains("- launch"),
        "Help text should list velocity launch as an available primitive.");
    assertTrue(availableText.contains("repair: Target Kind mainCharacter, allDamageable"),
        "Help text should explain repair target options.");
    assertTrue(availableText.contains("finishRun: Target Kind world; Target ID blank"),
        "Help text should explain world-only terminal effects.");
    assertTrue(availableText.contains("launch: Target Kind mainCharacter, vehicle"),
        "Help text should explain launch target options.");
    assertTrue(availableText.contains("- mainCharacter"),
        "Help text should list registered target kinds.");
    assertTrue(availableText.contains("componentId=<static component id>; optional"),
        "Help text should explain required spawnPowerup parameters.");
    assertTrue(availableText.contains("mainCharacterId: BigPig"),
        "Help text should include the current main character id.");
    assertTrue(availableText.contains("movable component ids: Frame"),
        "Help text should include movable component ids for componentType targets.");
    assertTrue(availableText.contains("static component ids for spawnPowerup componentId: Heart"),
        "Help text should include static component ids for spawnPowerup.");
    assertTrue(availableText.contains("terrain type ids: Grass"),
        "Help text should include terrain ids from the current catalog.");
    assertTrue(availableText.contains("runtime tag ids for tag targets:"),
        "Help text should include runtime tag ids for tag targets.");
  }
}
