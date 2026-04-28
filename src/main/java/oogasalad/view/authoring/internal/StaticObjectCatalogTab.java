package oogasalad.view.authoring.internal;

import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.StaticComponentInfo;

/**
 * Static object editor tab for the game catalog authoring screen.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class StaticObjectCatalogTab {

  private static final double DEFAULT_WIDTH = 220;
  private static final double FORM_ROW_WIDTH = 120;
  private static final double FORM_ROW_SPACING = 6;
  private static final double FORM_ROW_PADDING = 8;
  private static final double CONTENT_PADDING = 8;
  private static final double SPRITE_ROW_SPACING = 8;
  private static final double BUTTON_BAR_SPACING = 8;
  private static final double DEFAULT_MASS = 1.0;
  private static final double DEFAULT_POWER_UP_MASS = 0;
  private static final double LIST_WIDTH = 260;
  private static final double TAB_CONTENT_SPACING = 12;
  private static final double DEFAULT_AMOUNT = 0;

  private final GameCatalogDraft draft;
  private final SpriteImportService spriteImportService;
  private final AuthoringAlertService alertService;

  private ListView<String> staticListView;

  StaticObjectCatalogTab(GameCatalogDraft draft, SpriteImportService spriteImportService,
      AuthoringAlertService alertService) {
    this.draft = Objects.requireNonNull(draft);
    this.spriteImportService = Objects.requireNonNull(spriteImportService);
    this.alertService = Objects.requireNonNull(alertService);
  }

  Tab build() {
    staticListView = new ListView<>();
    staticListView.setId("static-component-list");
    refresh();

    TextField idF = AuthoringValueHints.newTextField("catalog.static.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.static.spriteUrl");
    TextField massF = AuthoringValueHints.newTextField("catalog.static.mass");
    TextField amountF = AuthoringValueHints.newTextField("catalog.static.amount");

    ComboBox<String> typeDropdown = new ComboBox<>();
    typeDropdown.getItems().addAll("Decoration", "HP Object", "Score Object", "Finish Line");
    typeDropdown.setValue("Decoration");
    typeDropdown.setId("static-type-dropdown");

    Button addButton = new Button("Add");
    addButton.setOnAction(e -> {
      String selected = typeDropdown.getValue();
      if (selected == null) {
        alertService.showError("No Type", "Please select a type from the dropdown.");
        return;
      }
      if ("Decoration".equals(selected)) {
        draft.staticComponents().add(new StaticComponentInfo(
            idF.getText().strip(),
            spriteF.getText().strip(),
            parseDouble(massF.getText(), DEFAULT_MASS),
            null));
      } else if ("HP Object".equals(selected)) {
        double hpChange = parseDouble(amountF.getText(), DEFAULT_AMOUNT);
        if (hpChange == DEFAULT_AMOUNT) {
          alertService.showError("Invalid HP",
              "HP change must be nonzero. Use positive to heal, negative to damage.");
          return;
        }
        draft.staticComponents().add(new StaticComponentInfo(
            idF.getText().strip(),
            spriteF.getText().strip(),
            parseDouble(massF.getText(), DEFAULT_MASS),
            buildHpAction(hpChange)));
      } else if ("Score Object".equals(selected)) {
        double scoreAmount = parseDouble(amountF.getText(), DEFAULT_AMOUNT);
        if (scoreAmount <= DEFAULT_AMOUNT) {
          alertService.showError("Invalid Score",
              "Score amount must be greater than zero.");
          return;
        }
        draft.staticComponents().add(new StaticComponentInfo(
            idF.getText().strip(),
            spriteF.getText().strip(),
            parseDouble(massF.getText(), DEFAULT_MASS),
            new CollisionAction("score", scoreAmount)));
      } else if ("Finish Line".equals(selected)) {
        String finishId = idF.getText().strip().isBlank() ? "GoalLine" : idF.getText().strip();
        draft.staticComponents().add(new StaticComponentInfo(
            finishId,
            spriteF.getText().strip(),
            DEFAULT_POWER_UP_MASS,
            new CollisionAction("finish", 0)));
      }
      refresh();
      clearFields(idF, spriteF, massF, amountF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> selectedIndex(staticListView, draft.staticComponents().size())
        .ifPresent(index -> {
          draft.staticComponents().remove(index.intValue());
          refresh();
        }));

    return new Tab("Static Objects", buildTabContent(staticListView,
        formRow("ID", idF), spriteRow(spriteF), formRow("Mass", massF),
        formRow("Type", typeDropdown), formRow("Amount", amountF),
        buttonBar(addButton, remove)));
  }

  void refresh() {
    if (staticListView == null) {
      return;
    }
    staticListView.getItems().setAll(draft.staticComponents().stream()
        .map(component -> component.id() + formatHpChange(component.onCollisionAction()))
        .toList());
  }

  private CollisionAction buildHpAction(double hpChange) {
    if (hpChange > DEFAULT_AMOUNT) {
      return new CollisionAction("heal", hpChange);
    }
    return new CollisionAction("damage", Math.abs(hpChange));
  }

  private String formatHpChange(CollisionAction action) {
    if (action == null) {
      return "  [inert]";
    }
    if ("finish".equals(action.type())) {
      return "  [finish line]";
    }
    if ("heal".equals(action.type())) {
      return "  [+" + (int) action.amount() + " HP]";
    }
    if ("damage".equals(action.type())) {
      return "  [-" + (int) action.amount() + " HP]";
    }
    if ("score".equals(action.type())) {
      return "  [+" + (int) action.amount() + " score]";
    }
    return "  [" + action.type() + "]";
  }

  private HBox spriteRow(TextField spriteField) {
    Button browse = new Button("Browse...");
    browse.setOnAction(e -> spriteImportService.browseSprite(browse, spriteField));
    Label label = new Label("Sprite:");
    label.setPrefWidth(DEFAULT_WIDTH);
    HBox.setHgrow(spriteField, Priority.ALWAYS);
    return new HBox(SPRITE_ROW_SPACING, label, spriteField, browse);
  }

  private HBox formRow(String labelText, TextField field) {
    Label label = new Label(labelText + ":");
    label.setPrefWidth(DEFAULT_WIDTH);
    return new HBox(FORM_ROW_WIDTH, label, field);
  }

  private HBox formRow(String labelText, ComboBox<String> comboBox) {
    Label label = new Label(labelText + ":");
    label.setPrefWidth(DEFAULT_WIDTH);
    return new HBox(FORM_ROW_WIDTH, label, comboBox);
  }

  private HBox buttonBar(Button... buttons) {
    HBox bar = new HBox(BUTTON_BAR_SPACING, buttons);
    bar.setAlignment(Pos.CENTER_RIGHT);
    bar.getStyleClass().add("game-authoring-action-row");
    return bar;
  }

  private Node buildTabContent(ListView<String> list, Node... formNodes) {
    list.setPrefWidth(LIST_WIDTH);
    VBox form = new VBox(FORM_ROW_SPACING, formNodes);
    form.setPadding(new Insets(FORM_ROW_PADDING));
    HBox content = new HBox(TAB_CONTENT_SPACING, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(CONTENT_PADDING));
    return content;
  }

  private double parseDouble(String text, double fallback) {
    try {
      return Double.parseDouble(text.strip());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private void clearFields(TextField... fields) {
    for (TextField field : fields) {
      field.clear();
    }
  }

  private Optional<Integer> selectedIndex(ListView<String> list, int size) {
    int index = list.getSelectionModel().getSelectedIndex();
    return index >= 0 && index < size ? Optional.of(index) : Optional.empty();
  }
}
