package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.GameCatalogSaver;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * TestFX workflows for authoring game-catalog cheat keys.
 *
 * @author Emilio Garcia
 */
class GameAuthoringScreenImplCheatTest extends DukeApplicationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final List<GameConfig> capturedConfigs = new ArrayList<>();
  private final List<Outcome> capturedOutcomes = new ArrayList<>();

  private GameAuthoringScreenImpl screen;
  private Path sourceCatalog;
  private Path gamesDir;
  private Path levelsDir;

  @Override
  public void start(Stage stage) throws IOException {
    Path testRoot = Files.createTempDirectory("game-authoring-cheats");
    gamesDir = testRoot.resolve("games");
    levelsDir = testRoot.resolve("levels");
    sourceCatalog = writeCatalog(testRoot.resolve("source-game.json"), "[]");
    screen = new GameAuthoringScreenImpl(capturedOutcomes::add, path -> {
    }, capturedConfigs::add, new GameCatalogSaver(), gamesDir, levelsDir,
        new FileChooserService());
    stage.setScene(new Scene(screen.getRoot(), 1200, 800));
    stage.show();
  }

  @Test
  void CheatsTab_NewSpawnPowerupCheat_SavesCheatToCatalog() throws IOException {
    runAsJFXAction(() -> screen.loadCatalogFromPath(sourceCatalog.toString()));
    clickOn("Cheats");

    writeField("cheat-effect-type-field", "spawnPowerup");
    writeField("cheat-target-kind-field", "world");
    writeField("cheat-parameters-field", "componentId=heart,x=400,y=600");
    clickOn("Add Effect");
    assertEquals(1, stringList("cheat-effect-list").getItems().size(),
        "Adding an effect should stage it before creating the cheat.");

    writeField("cheat-id-field", "spawn_heart");
    writeField("cheat-name-field", "Spawn Heart");
    writeField("cheat-keys-field", "SHIFT,P");
    clickOn("Add Cheat");
    assertTrue(stringList("cheat-list").getItems().getFirst().contains("spawn_heart"),
        "Adding a cheat should display it in the cheat list.");

    clickOn("Create a New Level");

    JsonNode cheatNode = readSavedCatalog().path("cheats").get(0);
    JsonNode effectNode = cheatNode.path("effects").get(0);
    assertEquals("spawn_heart", cheatNode.path("id").asText(),
        "Saving the catalog should persist the author-created cheat id.");
    assertEquals("SHIFT", cheatNode.path("trigger").path("keys").get(0).asText(),
        "Saving the catalog should preserve the first key in the chord.");
    assertEquals("spawnPowerup", effectNode.path("type").asText(),
        "Saving the catalog should persist the selected primitive effect.");
    assertEquals("heart", effectNode.path("parameters").path("componentId").asText(),
        "Saving the catalog should persist effect parameters.");
    assertEquals("spawn_heart", capturedConfigs.getFirst().cheats().getFirst().id(),
        "Navigating to level authoring should pass a config containing the new cheat.");
    assertEquals(Outcome.OPEN_AUTHORING, capturedOutcomes.getFirst(),
        "Navigating after saving should open the level authoring screen.");
  }

  @Test
  void CheatsTab_LoadedCatalogWithCheat_PreservesCheatOnSave() throws IOException {
    sourceCatalog = writeCatalog(sourceCatalog.getParent().resolve("catalog-with-cheat.json"), """
        [
          {
            "id": "repair_body",
            "name": "Repair Body",
            "enabled": true,
            "trigger": { "keys": ["SHIFT", "R"] },
            "effects": [
              {
                "type": "repair",
                "target": { "kind": "mainCharacter", "id": "" },
                "parameters": { "amount": 50 }
              }
            ]
          }
        ]
        """);

    runAsJFXAction(() -> screen.loadCatalogFromPath(sourceCatalog.toString()));
    clickOn("Cheats");

    assertTrue(stringList("cheat-list").getItems().getFirst().contains("repair_body"),
        "Loading a catalog with cheats should display the existing cheat.");

    clickOn("Create a New Level");

    JsonNode cheatNode = readSavedCatalog().path("cheats").get(0);
    assertEquals("repair_body", cheatNode.path("id").asText(),
        "Saving a loaded catalog should not drop existing cheats.");
    assertEquals("repair", cheatNode.path("effects").get(0).path("type").asText(),
        "Saving a loaded catalog should preserve existing cheat effects.");
  }

  @Test
  void CheatsTab_HelpButton_IsAvailableForAuthorGuidance() {
    clickOn("Cheats");

    Button helpButton = lookup("#cheat-help-button").queryAs(Button.class);

    assertEquals("Cheat Help", helpButton.getText(),
        "Cheats tab should expose a help button for examples and registry options.");
  }

  @Test
  void Root_GameAuthoringScreen_LoadsScopedThemeStylesheet() {
    assertTrue(screen.getRoot().getStyleClass().contains("game-authoring-root"),
        "Game authoring should scope custom styles to its own root class.");
    assertTrue(screen.getRoot().getStylesheets().stream()
            .anyMatch(stylesheet -> stylesheet.endsWith("game-authoring.css")),
        "Game authoring should load the screen-specific stylesheet.");
  }

  @Test
  void CheatsTab_ActionRows_AreAlignedBottomRight() {
    clickOn("Cheats");

    HBox actionRow = lookup(".game-authoring-action-row").queryAs(HBox.class);

    assertEquals(Pos.CENTER_RIGHT, actionRow.getAlignment(),
        "Game authoring tab action rows should align buttons to the right.");
  }

  private void writeField(String id, String text) {
    writeInputTo(lookup("#" + id).queryAs(TextField.class), text);
  }

  @SuppressWarnings("unchecked")
  private ListView<String> stringList(String id) {
    return (ListView<String>) lookup("#" + id).queryAs(ListView.class);
  }

  private JsonNode readSavedCatalog() throws IOException {
    return MAPPER.readTree(gamesDir.resolve("g").resolve("game.json").toFile());
  }

  private Path writeCatalog(Path path, String cheatsJson) throws IOException {
    Files.writeString(path, """
        {
          "gameId": "g",
          "mainCharacterId": "body",
          "components": [
            { "id": "body", "spriteUrl": "body.png", "mass": 1.0, "buttons": [] }
          ],
          "staticComponents": [
            {
              "id": "heart",
              "spriteUrl": "heart.png",
              "mass": 0,
              "onCollisionAction": { "type": "heal", "amount": 25 }
            }
          ],
          "terrainTypes": [],
          "cheats": %s
        }
        """.formatted(cheatsJson));
    return path;
  }
}
