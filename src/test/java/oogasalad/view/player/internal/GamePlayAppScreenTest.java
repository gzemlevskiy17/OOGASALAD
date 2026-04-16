package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

class GamePlayAppScreenTest extends DukeApplicationTest {

  private final List<Outcome> capturedOutcomes = new ArrayList<>();
  private GamePlayAppScreen screen;

  @Override
  public void start(Stage stage) {
    screen = new GamePlayAppScreen(capturedOutcomes::add);
    stage.setScene(new Scene(screen.getRoot(), 1280, 720));
    stage.show();
  }

  @Test
  void gameLayer_isPresentAndStyled() {
    Pane layer = screen.getGameLayer();
    assertTrue(layer.getStyleClass().contains("game-container"));
  }

  @Test
  void backButton_invokesOnOutcomeWithBackToVehicleBuilder() {
    clickOn("Back to the Vehicle Construction");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.BACK_TO_VEHICLE_BUILDER, capturedOutcomes.get(0));
  }

  @Test
  void tempWinButton_invokesGameWon() {
    clickOn("TEMP WIN");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.GAME_WON, capturedOutcomes.get(0));
  }

  @Test
  void tempLoseButton_invokesGameLost() {
    clickOn("TEMP LOSE");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.GAME_LOST, capturedOutcomes.get(0));
  }

  @Test
  void resetButton_invokesOnReset() {
    AtomicInteger resets = new AtomicInteger();
    runAsJFXAction(() -> screen.setOnReset(resets::incrementAndGet));
    clickOn("Reset");
    assertEquals(1, resets.get());
  }

  @Test
  void pauseToggle_firstSelectInvokesPause_secondClickInvokesResume() {
    AtomicInteger pauses = new AtomicInteger();
    AtomicInteger resumes = new AtomicInteger();
    runAsJFXAction(() -> {
      screen.setOnPause(pauses::incrementAndGet);
      screen.setOnResume(resumes::incrementAndGet);
    });

    clickOn("Pause");
    assertEquals(1, pauses.get(), "First toggle should pause");
    assertEquals(0, resumes.get());

    clickOn("Pause");
    assertEquals(1, pauses.get());
    assertEquals(1, resumes.get(), "Second toggle should resume");
  }

  @Test
  void toolToggle_selectThenDeselect_invokesActivateThenDeactivateWithToolIds() {
    List<String> activated = new ArrayList<>();
    List<String> deactivated = new ArrayList<>();
    runAsJFXAction(() -> {
      screen.setOnToolActivated(activated::add);
      screen.setOnToolDeactivated(deactivated::add);
    });

    clickOn("Engine");
    assertEquals(List.of("engine"), activated);
    assertTrue(deactivated.isEmpty());

    clickOn("Engine");
    assertEquals(List.of("engine"), deactivated);
  }
}