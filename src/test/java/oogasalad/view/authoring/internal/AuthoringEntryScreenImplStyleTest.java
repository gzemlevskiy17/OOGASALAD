package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import oogasalad.data.internal.LevelScanner;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * TestFX checks for authoring-entry screen theme isolation.
 *
 * @author Emilio Garcia
 */
class AuthoringEntryScreenImplStyleTest extends DukeApplicationTest {

  private AuthoringEntryScreenImpl screen;

  @Override
  public void start(Stage stage) {
    Map<String, List<String>> worlds = new LinkedHashMap<>();
    worlds.put("forest", List.of("level1"));
    screen = new AuthoringEntryScreenImpl(
        outcome -> {
        },
        new LevelScanner.ScanResult(worlds, List.of()),
        world -> {
        },
        level -> {
        });
    stage.setScene(new Scene(screen.getRoot(), 900, 700));
    stage.show();
    screen.onEnter();
  }

  @Test
  void Root_AuthoringEntry_LoadsOnlyAuthoringEntryStylesheet() {
    Parent content = (Parent) ((ScrollPane) screen.getRoot()).getContent();

    assertTrue(content.getStylesheets().stream()
            .anyMatch(stylesheet -> stylesheet.endsWith("authoring-entry.css")),
        "Authoring entry should load its own stable stylesheet.");
    assertFalse(content.getStylesheets().stream()
            .anyMatch(stylesheet -> stylesheet.endsWith("player-entry.css")),
        "Authoring entry should not load player-entry styles that resize world buttons later.");
  }

  @Test
  void WorldButton_AuthoringEntryRendered_UsesAuthoringWorldButtonClass() {
    assertTrue(lookup(".world-button").query().getStyleClass().contains("world-button"),
        "Authoring entry should still render world buttons after removing player-entry CSS.");
  }
}
