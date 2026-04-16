package oogasalad.view.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

class LoseAppScreenTest extends DukeApplicationTest {

  private final List<Outcome> capturedOutcomes = new ArrayList<>();

  @Override
  public void start(Stage stage) {
    LoseAppScreen screen = new LoseAppScreen(capturedOutcomes::add);
    stage.setScene(new Scene(screen.getRoot(), 1280, 720));
    stage.show();
  }

  @Test
  void title_showsLoseMessage() {
    Label title = lookup(".result-title").queryAs(Label.class);
    assertEquals("You lose !!!!!!!!!!!!!!!!!", title.getText());
  }

  @Test
  void homeButton_invokesOnOutcomeWithBackToMenu() {
    clickOn("Home");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.BACK_TO_MENU, capturedOutcomes.get(0));
  }

  @Test
  void tryAgainButton_invokesOnOutcomeWithOpenGameplay() {
    clickOn("Try again");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.OPEN_GAMEPLAY, capturedOutcomes.get(0));
  }

  @Test
  void playSomethingButton_invokesOnOutcomeWithOpenPlayerEntry() {
    clickOn("Play something...");
    assertEquals(1, capturedOutcomes.size());
    assertEquals(Outcome.OPEN_PLAYER_ENTRY, capturedOutcomes.get(0));
  }
}