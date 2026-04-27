package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.player.internal.DefaultBuildRulesRegistry;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.SpriteCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.DukeApplicationTest;

class VehicleBuilderAppScreenTest extends DukeApplicationTest {

  private final List<Outcome> capturedOutcomes = new ArrayList<>();
  @TempDir
  Path tempDir;
  private VehicleBuilderAppScreen screen;

  @Override
  public void start(Stage stage) {
    screen = new VehicleBuilderAppScreen(capturedOutcomes::add,
        new SpriteCache(), new PaletteSpriteRegistry(), new DefaultBuildRulesRegistry());
    stage.setScene(new Scene(screen.getRoot(), 1000, 800));
    stage.show();
  }

  @Test
  void title_showsCreateYourVehicle() {
    Label title = lookup(".header-title").queryAs(Label.class);
    assertEquals("Create your vehicle", title.getText());
  }

  @Test
  void backButton_invokesOnOutcomeWithBackToPlayerEntry() {
    clickOn("Back to Level Choosing Screen");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.BACK_TO_PLAYER_ENTRY, capturedOutcomes.get(0));
  }

  @Test
  void startButton_invokesOnOutcomeWithOpenGameplay() {
    clickOn("START THE LEVEL");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.OPEN_GAMEPLAY, capturedOutcomes.get(0));
  }

  @Test
  void startButton_requiresConfiguredMainCharacterInBuild() throws IOException {
    Path gameFile = tempDir.resolve("game.json");
    Files.writeString(gameFile, """
        {"gameId":"test","mainCharacterId":"Pig",
         "components":[],"staticComponents":[],"terrainTypes":[]}
        """);
    screen.setGameConfig(new GameConfigImpl(gameFile.toString()));

    clickOn("START THE LEVEL");

    assertTrue(capturedOutcomes.isEmpty());
    Label error = lookup(".build-error").queryAs(Label.class);
    assertEquals("not able to start the game", error.getText());
  }

  @Test
  void getVehicleLayout_initiallyEmpty() {
    assertTrue(screen.getVehicleLayout().placements().isEmpty());
  }

  @Test
  void dragComponentFromPaletteToGrid_recordsPlacement() {
    Node source = lookup(".component-cell").nth(0).query();
    Node target = lookup(".grid-cell").nth(0).query();
    drag(source).dropTo(target);
    assertEquals(
        "Pig",
        screen.getVehicleLayout().getComponentAtIndex(0, 0).orElseThrow());
  }
}