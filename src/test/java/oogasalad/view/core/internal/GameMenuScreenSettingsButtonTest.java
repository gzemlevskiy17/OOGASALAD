package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * Failing test for the "Settings button has no action handler" bug.
 * See doc/bugs/SETTINGS_BUTTON_BUG.md.
 *
 * @author Mason Wu
 */
class GameMenuScreenSettingsButtonTest extends DukeApplicationTest {

  private GameMenuScreen screen;

  @Override
  public void start(Stage stage) {
    screen = new GameMenuScreen(outcome -> {}, accountOutcome -> {}, () -> false);
    stage.setScene(new Scene(screen.getRoot(), 1000, 700));
    stage.show();
  }

  @Test
  void settingsButton_HasOnActionHandler_BugFix() {
    Button settingsButton = lookup("Settings").queryButton();
    assertNotNull(settingsButton.getOnAction(),
        "Settings button has no action handler — clicking it does nothing.");
  }
}
