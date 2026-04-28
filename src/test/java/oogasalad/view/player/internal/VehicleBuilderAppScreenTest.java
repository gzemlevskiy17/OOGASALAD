package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.model.player.internal.DefaultBuildRulesRegistry;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.SpriteCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.util.WaitForAsyncUtils;
import util.DukeApplicationTest;

/**
 * TestFX workflows for the vehicle builder screen.
 */
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
  void Title_DefaultScreen_DisplaysCreateYourVehicle() {
    Label title = lookup(".header-title").queryAs(Label.class);

    assertEquals("Create your vehicle", title.getText());
  }

  @Test
  void BackButton_Clicked_EmitsBackToPlayerEntry() {
    clickOn("Back to Level Choosing Screen");

    assertEquals(List.of(Outcome.BACK_TO_PLAYER_ENTRY), capturedOutcomes);
  }

  @Test
  void StartButton_NoGameConfig_EmitsOpenGameplay() {
    clickOn("START THE LEVEL");

    assertEquals(List.of(Outcome.OPEN_GAMEPLAY), capturedOutcomes);
  }

  @Test
  void StartButton_ConfiguredMainCharacterMissing_ShowsErrorAndDoesNotEmitOutcome()
      throws IOException {
    runAsJFXAction(() -> screen.setGameConfig(loadGameConfigWithMainCharacter("Pig")));

    clickOn("START THE LEVEL");

    assertTrue(capturedOutcomes.isEmpty());
    Label error = lookup(".build-error").queryAs(Label.class);
    assertEquals("not able to start the game", error.getText());
  }

  @Test
  void DragComponent_FrameDroppedOnGrid_RecordsFramePlacement() {
    dragComponentToFirstGridCell("Frame");

    assertEquals("Frame", screen.getVehicleLayout().getComponentAtIndex(0, 0).orElseThrow());
  }

  @Test
  void StartButton_ConfiguredPigStackedInsideFrame_EmitsOpenGameplay() throws IOException {
    runAsJFXAction(() -> screen.setGameConfig(loadGameConfigWithMainCharacter("Pig")));
    dragComponentToFirstGridCell("Frame");
    dragComponentToFirstGridCell("Pig");

    clickOn("START THE LEVEL");

    assertEquals(List.of(Outcome.OPEN_GAMEPLAY), capturedOutcomes);
  }

  @Test
  void OnEnter_PreviousGridContentsExist_ClearsVisualGridAndLayout() {
    StackPane cell = firstGridCell();
    dragComponentToFirstGridCell("Frame");
    assertFalse(cell.getChildren().isEmpty());

    runAsJFXAction(screen::onEnter);

    assertTrue(screen.getVehicleLayout().placements().isEmpty());
    assertTrue(cell.getChildren().isEmpty());
  }

  @Test
  void SetAllowedComponentIds_PreviousGridContentsExist_ClearsVisualGridAndLayout() {
    StackPane cell = firstGridCell();
    dragComponentToFirstGridCell("Frame");
    assertFalse(cell.getChildren().isEmpty());

    runAsJFXAction(() -> screen.setAllowedComponentIds(List.of("Frame", "Pig")));

    assertTrue(screen.getVehicleLayout().placements().isEmpty());
    assertTrue(cell.getChildren().isEmpty());
  }

  @Test
  void setAllowedComponentIds_unknownSpriteId_keepsPaletteVisible() {
    runAsJFXAction(() -> screen.setAllowedComponentIds(List.of("block")));

    Label label = lookup("block").queryAs(Label.class);

    assertEquals("block", label.getText());
  }

  private void dragComponentToFirstGridCell(String componentName) {
    drag(componentCell(componentName)).dropTo(firstGridCell());
    WaitForAsyncUtils.waitForFxEvents();
  }

  private Node componentCell(String componentName) {
    return lookup(".component-cell").queryAll().stream()
        .filter(node -> componentLabel(node).getText().equals(componentName))
        .findFirst()
        .orElseThrow();
  }

  private Label componentLabel(Node componentCell) {
    return (Label) componentCell.lookup(".label");
  }

  private StackPane firstGridCell() {
    return lookup(".grid-cell").nth(0).queryAs(StackPane.class);
  }

  private GameConfigImpl loadGameConfigWithMainCharacter(String mainCharacterId) {
    Path gameFile = tempDir.resolve(mainCharacterId + "-game.json");
    try {
      Files.writeString(gameFile, """
          {"gameId":"test","mainCharacterId":"%s",
           "components":[],"staticComponents":[],"terrainTypes":[]}
          """.formatted(mainCharacterId));
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create test game config", exception);
    }
    return new GameConfigImpl(gameFile.toString());
  }
}
