package oogasalad.view.authoring.internal.entry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Scene;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.LevelScanner;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * TestFX workflows for authoring-entry navigation callbacks.
 *
 * @author Emilio Garcia
 */
class AuthoringEntryScreenImplNavigationTest extends DukeApplicationTest {

  private final List<String> events = new ArrayList<>();

  private AuthoringEntryScreenImpl screen;

  @Override
  public void start(Stage stage) {
    Map<String, List<String>> worlds = new LinkedHashMap<>();
    worlds.put("forest", List.of("level1"));
    LevelScanner.ScanResult scanResult = new LevelScanner.ScanResult(worlds, List.of());
    screen = new AuthoringEntryScreenImpl(
        outcome -> events.add("outcome:" + outcome),
        () -> scanResult,
        world -> events.add("level-world:" + world),
        level -> events.add("level:" + level),
        world -> events.add("edit-world:" + world),
        () -> events.add("new-world"));
    stage.setScene(new Scene(screen.getRoot(), 900, 700));
    stage.show();
    screen.onEnter();
  }

  @Test
  void EditExistingWorld_SelectedWorld_ConfiguresWorldBeforeNavigation() {
    clickOn("Enchanted Forest");
    clickOn("Edit Existing World");

    assertEquals(List.of("edit-world:forest", "outcome:" + Outcome.OPEN_GAME_AUTHORING), events,
        "Editing an existing world should configure the selected world before navigation.");
  }

  @Test
  void AddNewWorld_ButtonPressed_ResetsCatalogBeforeNavigation() {
    clickOn("Add a New World");

    assertEquals(List.of("new-world", "outcome:" + Outcome.OPEN_GAME_AUTHORING), events,
        "Adding a new world should reset game authoring before navigation.");
  }
}
