package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

class PlayerEntryAppScreenTest extends DukeApplicationTest {

  private final List<Outcome> capturedOutcomes = new ArrayList<>();
  private final List<CapturedSelection> capturedSelections = new ArrayList<>();

  private PlayerEntryAppScreen screen;

  private record CapturedSelection(String worldKey, String levelKey) {}

  @Override
  public void start(Stage stage) {
    screen = new PlayerEntryAppScreen(
        capturedOutcomes::add,
        (worldKey, levelKey) ->
            capturedSelections.add(new CapturedSelection(worldKey, levelKey)));
    stage.setScene(new Scene(screen.getRoot(), 1280, 720));
    stage.show();
  }

  @Test
  void backButton_invokesOnOutcomeWithBackToMenu() {
    clickOn("Back to Menu");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.BACK_TO_MENU, capturedOutcomes.get(0));
    assertTrue(capturedSelections.isEmpty());
  }

  @Test
  void startButton_disabledUntilWorldAndLevelChosen() {
    Button start = lookup("LAUNCH GAME").queryButton();
    assertTrue(start.isDisabled());

    clickOn("Enchanted Forest");
    assertTrue(start.isDisabled());

    clickOn("1");
    assertFalse(start.isDisabled());
  }

  @Test
  void launchGame_invokesOnSelectionConfirmedThenOnOutcome() {
    clickOn("Dusty Dunes");
    clickOn("2");
    clickOn("LAUNCH GAME");

    assertEquals(1, capturedSelections.size());
    assertEquals("desert", capturedSelections.get(0).worldKey());
    assertEquals("second", capturedSelections.get(0).levelKey());

    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.OPEN_VEHICLE_BUILDER, capturedOutcomes.get(0));
  }

  @Test
  void lockedLevelButtons_areDisabled() {
    Button fourth = lookup("4").queryButton();
    Button fifth = lookup("5").queryButton();
    assertTrue(fourth.isDisabled());
    assertTrue(fifth.isDisabled());
  }
}