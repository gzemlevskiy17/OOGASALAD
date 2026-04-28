package oogasalad.view.authoring.internal.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.LevelScanner;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * Tests that selecting a world and clicking "Edit Existing World" passes the selected world key so
 * the game catalog can be auto-loaded without requiring the user to manually upload a game.json
 * file.
 *
 * @author Paulina Vvedenskaya
 */
class AuthoringEntryEditExistingWorldTest extends DukeApplicationTest {

  private final List<Outcome> capturedOutcomes = new ArrayList<>();
  private final List<String> capturedWorldKeys = new ArrayList<>();
  private AuthoringEntryScreenImpl screen;

  @Override
  public void start(Stage stage) {
    LevelScanner.ScanResult scanResult = new LevelScanner.ScanResult(
        Map.of("badpiggies", List.of("level1", "level2")),
        List.of());

    screen = new AuthoringEntryScreenImpl(
        capturedOutcomes::add,
        scanResult,
        capturedWorldKeys::add,
        path -> {
        });

    stage.setScene(new Scene(screen.getRoot(), 1200, 800));
    stage.show();
    interact(() -> screen.onEnter());
  }

  @Test
  void editExistingWorld_passesSelectedWorldKey() {
    Button worldButton = lookup(".world-badpiggies").queryAs(Button.class);
    clickOn(worldButton);

    Button editButton = lookup(".authoring-entry-action").queryAs(Button.class);
    clickOn(editButton);

    assertFalse(capturedWorldKeys.isEmpty(),
        "Clicking 'Edit Existing World' should pass the selected world key "
            + "so the game catalog can be auto-loaded, but no world key was received");
    assertEquals("badpiggies", capturedWorldKeys.get(0),
        "The world key passed should match the selected world");
  }

  @Test
  void editExistingWorld_withoutSelection_buttonIsDisabled() {
    Button editButton = lookup(".authoring-entry-action").queryAs(Button.class);
    assertTrue(editButton.isDisabled(),
        "'Edit Existing World' should be disabled when no world is selected");
  }
}
