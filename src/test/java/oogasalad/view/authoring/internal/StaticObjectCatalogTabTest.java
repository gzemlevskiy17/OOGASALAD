package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.StaticComponentInfo;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;
import util.DukeApplicationTest;

/**
 * Tests for the static object catalog tab dropdown and single Add button.
 *
 * @author Paulina Vvedenskaya
 */
class StaticObjectCatalogTabTest extends DukeApplicationTest {

  private GameCatalogDraft draft;
  private final List<String> alertTitles = new ArrayList<>();
  private Tab staticTab;

  @Override
  public void start(Stage stage) {
    draft = new GameCatalogDraft();
    AuthoringAlertService alertService = new AuthoringAlertService() {
      @Override
      public void showError(String title, String message) {
        alertTitles.add(title);
      }
    };
    SpriteImportService spriteService = new SpriteImportService(
        Path.of("data/assets"), alertService);
    StaticObjectCatalogTab tabBuilder = new StaticObjectCatalogTab(
        draft, spriteService, alertService);
    staticTab = tabBuilder.build();
    TabPane tabPane = new TabPane(staticTab);
    stage.setScene(new Scene(tabPane, 800, 600));
    stage.show();
  }

  @SuppressWarnings("unchecked")
  private ComboBox<String> typeDropdown() {
    return (ComboBox<String>) staticTab.getContent().lookup("#static-type-dropdown");
  }

  @SuppressWarnings("unchecked")
  private ListView<String> catalogList() {
    return (ListView<String>) staticTab.getContent().lookup("#static-component-list");
  }

  private TextField fieldByPrompt(String promptSubstring) {
    for (Object node : staticTab.getContent().lookupAll(".text-field")) {
      if (node instanceof TextField tf) {
        String prompt = tf.getPromptText();
        if (prompt != null && prompt.toLowerCase().contains(promptSubstring.toLowerCase())) {
          return tf;
        }
      }
    }
    throw new AssertionError("No TextField with prompt containing: " + promptSubstring);
  }

  private void fillIdAndSprite(String id) {
    TextField idField = fieldByPrompt("id");
    writeInputTo(idField, id);
    TextField spriteField = fieldByPrompt("sprite");
    writeInputTo(spriteField, "data/assets/test.png");
  }

  private void selectTypeAndAdd(String type) {
    select(typeDropdown(), type);
    WaitForAsyncUtils.waitForFxEvents();
    clickOn((Button) lookup("Add").query());
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  void dropdown_defaultsToDecoration() {
    assertEquals("Decoration", typeDropdown().getValue());
  }

  @Test
  void dropdown_containsAllFourTypes() {
    List<String> items = typeDropdown().getItems();
    assertEquals(4, items.size());
    assertTrue(items.contains("Decoration"));
    assertTrue(items.contains("HP Object"));
    assertTrue(items.contains("Score Object"));
    assertTrue(items.contains("Finish Line"));
  }

  @Test
  void addDecoration_createsInertStaticComponent() {
    fillIdAndSprite("Rock");
    selectTypeAndAdd("Decoration");

    assertEquals(1, draft.staticComponents().size());
    StaticComponentInfo added = draft.staticComponents().get(0);
    assertEquals("Rock", added.id());
    assertNull(added.onCollisionAction());
  }

  @Test
  void addHpObject_positiveAmount_createsHealAction() {
    fillIdAndSprite("Heart");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "25");
    selectTypeAndAdd("HP Object");

    assertEquals(1, draft.staticComponents().size());
    CollisionAction action = draft.staticComponents().get(0).onCollisionAction();
    assertEquals("heal", action.type());
    assertEquals(25.0, action.amount());
  }

  @Test
  void addHpObject_negativeAmount_createsDamageAction() {
    fillIdAndSprite("Spike");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "-10");
    selectTypeAndAdd("HP Object");

    assertEquals(1, draft.staticComponents().size());
    CollisionAction action = draft.staticComponents().get(0).onCollisionAction();
    assertEquals("damage", action.type());
    assertEquals(10.0, action.amount());
  }

  @Test
  void addHpObject_zeroAmount_showsErrorAndDoesNotAdd() {
    fillIdAndSprite("BadHp");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "0");
    selectTypeAndAdd("HP Object");

    assertEquals(0, draft.staticComponents().size());
    assertTrue(alertTitles.contains("Invalid HP"));
  }

  @Test
  void addScoreObject_positiveAmount_createsScoreAction() {
    fillIdAndSprite("Coin");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "100");
    selectTypeAndAdd("Score Object");

    assertEquals(1, draft.staticComponents().size());
    CollisionAction action = draft.staticComponents().get(0).onCollisionAction();
    assertEquals("score", action.type());
    assertEquals(100.0, action.amount());
  }

  @Test
  void addScoreObject_zeroAmount_showsErrorAndDoesNotAdd() {
    fillIdAndSprite("BadScore");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "0");
    selectTypeAndAdd("Score Object");

    assertEquals(0, draft.staticComponents().size());
    assertTrue(alertTitles.contains("Invalid Score"));
  }

  @Test
  void addFinishLine_createsFinishAction() {
    fillIdAndSprite("Goal");
    selectTypeAndAdd("Finish Line");

    assertEquals(1, draft.staticComponents().size());
    StaticComponentInfo added = draft.staticComponents().get(0);
    assertEquals("Goal", added.id());
    CollisionAction action = added.onCollisionAction();
    assertEquals("finish", action.type());
    assertEquals(0.0, action.amount());
  }

  @Test
  void addFinishLine_blankId_defaultsToGoalLine() {
    TextField idField = fieldByPrompt("id");
    writeInputTo(idField, "");
    selectTypeAndAdd("Finish Line");

    assertEquals(1, draft.staticComponents().size());
    assertEquals("GoalLine", draft.staticComponents().get(0).id());
  }

  @Test
  void addMultipleTypes_allAppearInList() {
    fillIdAndSprite("Rock");
    selectTypeAndAdd("Decoration");

    fillIdAndSprite("Heart");
    TextField amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "10");
    selectTypeAndAdd("HP Object");

    fillIdAndSprite("Coin");
    amountField = fieldByPrompt("amount");
    writeInputTo(amountField, "50");
    selectTypeAndAdd("Score Object");

    fillIdAndSprite("Flag");
    selectTypeAndAdd("Finish Line");

    assertEquals(4, draft.staticComponents().size());
    assertEquals(4, catalogList().getItems().size());
  }

  @Test
  void removeButton_deletesSelectedEntry() {
    fillIdAndSprite("Trash");
    selectTypeAndAdd("Decoration");
    assertEquals(1, draft.staticComponents().size());

    select(catalogList(), catalogList().getItems().get(0));
    WaitForAsyncUtils.waitForFxEvents();
    clickOn((Button) lookup("Remove").query());
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(0, draft.staticComponents().size());
  }
}
