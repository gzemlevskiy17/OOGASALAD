package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.internal.BuiltInVehicleCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.util.WaitForAsyncUtils;
import util.DukeApplicationTest;

/**
 * Black-box UI tests for {@link GameAuthoringScreenImpl} without adding test hooks to production.
 * <p>
 * The Components tab layout is assumed stable: {@code Tab} content is an {@link HBox} with the
 * catalog {@link ListView} at index 0 and the form {@link VBox} at index 1; the preset block is the
 * first child of the form ({@link VBox}), with the preset {@link ListView} at index 1 (between the
 * label and the button row).
 */
class GameAuthoringScreenImplTest extends DukeApplicationTest {

  private final List<Outcome> outcomes = new ArrayList<>();
  private GameAuthoringScreenImpl screen;

  @TempDir
  Path tempDir;

  @BeforeEach
  void clearOutcomes() {
    outcomes.clear();
  }

  @Override
  public void start(Stage stage) {
    screen = new GameAuthoringScreenImpl(outcomes::add, path -> {
    }, cfg -> {
    });
    stage.setScene(new Scene(screen.getRoot(), 960, 720));
    stage.show();
  }

  private TabPane catalogTabPane() {
    BorderPane root = (BorderPane) screen.getRoot();
    return (TabPane) root.getCenter();
  }

  private void openComponentsTab() {
    runAsJFXAction(() -> {
      TabPane tp = catalogTabPane();
      Tab t = tp.getTabs().stream()
          .filter(tab -> "Components".equals(tab.getText()))
          .findFirst()
          .orElseThrow();
      tp.getSelectionModel().select(t);
    });
    WaitForAsyncUtils.waitForFxEvents();
  }

  @SuppressWarnings("unchecked")
  private ListView<String> componentCatalogListView() {
    openComponentsTab();
    Tab components = catalogTabPane().getTabs().stream()
        .filter(t -> "Components".equals(t.getText()))
        .findFirst()
        .orElseThrow();
    HBox row = (HBox) components.getContent();
    return (ListView<String>) row.getChildren().get(0);
  }

  @SuppressWarnings("unchecked")
  private ListView<String> presetPickerListView() {
    openComponentsTab();
    Tab components = catalogTabPane().getTabs().stream()
        .filter(t -> "Components".equals(t.getText()))
        .findFirst()
        .orElseThrow();
    HBox row = (HBox) components.getContent();
    VBox form = (VBox) row.getChildren().get(1);
    VBox presetBlock = (VBox) form.getChildren().get(0);
    return (ListView<String>) presetBlock.getChildren().get(1);
  }

  @Test
  void header_displaysGameAuthoringTitle() {
    Label title = lookup(".authoring-entry-title").queryAs(Label.class);
    assertEquals("Game Authoring", title.getText());
  }

  @Test
  void catalogEditor_exposesTerrainStaticObjectsAndComponentsTabs() {
    TabPane pane = catalogTabPane();
    assertEquals(3, pane.getTabs().size());
    assertEquals("Terrain", pane.getTabs().get(0).getText());
    assertEquals("Static Objects", pane.getTabs().get(1).getText());
    assertEquals("Components", pane.getTabs().get(2).getText());
  }

  @Test
  void footer_includesSaveGameAndBackActions() {
    assertFalse(lookup("Save Game").queryAll().isEmpty());
    assertFalse(lookup("Back").queryAll().isEmpty());
  }

  @Test
  void backButton_requestsNavigateToAuthoringEntry() {
    clickOn((Button) lookup("Back").query());
    assertEquals(1, outcomes.size());
    assertEquals(Outcome.BACK_TO_AUTHORING_ENTRY, outcomes.get(0));
  }

  @Test
  void presetPicker_startsWithWoodWheelWhenCatalogEmpty() {
    assertTrue(presetPickerListView().getItems().contains("Wood Wheel"));
  }

  @Test
  void presetPicker_rowCountMatchesBuiltInNotYetInEmptyCatalog() {
    int expected = BuiltInVehicleCatalog.idsNotYetInCatalog(List.of()).size();
    assertEquals(expected, presetPickerListView().getItems().size());
  }

  @Test
  void addSelectedPresets_addsEngineRowToCatalogList() {
    openComponentsTab();
    select(presetPickerListView(), "Engine");
    clickOn("Add selected presets");
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(componentCatalogListView().getItems().stream()
        .anyMatch(row -> row.contains("Engine")));
  }

  @Test
  void addSelectedPresets_doesNotInsertDuplicateFrame() {
    openComponentsTab();
    ListView<String> presets = presetPickerListView();
    ListView<String> catalog = componentCatalogListView();
    select(presets, "Frame");
    clickOn("Add selected presets");
    WaitForAsyncUtils.waitForFxEvents();
    select(presets, "Frame");
    clickOn("Add selected presets");
    WaitForAsyncUtils.waitForFxEvents();
    long rows = catalog.getItems().stream()
        .filter(row -> row.startsWith("Frame"))
        .count();
    assertEquals(1, rows);
  }

  @Test
  void addSelectedPresets_removesIdFromPresetPicker() {
    openComponentsTab();
    select(presetPickerListView(), "Metal Wheel");
    clickOn("Add selected presets");
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(presetPickerListView().getItems().contains("Metal Wheel"));
  }

  @Test
  void loadCatalogFromPath_populatesCatalogListFromFile() throws IOException {
    Path gameJson = tempDir.resolve("game.json");
    Files.writeString(gameJson, """
        {
          "gameId": "fixturegame",
          "mainCharacterId": "BigPig",
          "components": [
            { "id": "OnlyPart", "spriteUrl": "", "mass": 2.0, "buttons": [] }
          ],
          "staticComponents": [],
          "terrainTypes": []
        }
        """);

    runAsJFXAction(() -> screen.loadCatalogFromPath(gameJson.toString()));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(componentCatalogListView().getItems().stream()
        .anyMatch(row -> row.contains("OnlyPart")));
  }
}