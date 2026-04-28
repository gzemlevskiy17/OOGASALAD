package oogasalad.view.authoring.internal;

import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import oogasalad.data.api.TerrainInfo;

/**
 * Terrain material editor tab for the game catalog authoring screen.
 *
 * @author Emilio Garcia
 */
final class TerrainCatalogTab {

  private static final double DEFAULT_WIDTH = 220;
  private static final double FORM_ROW_WIDTH = 120;
  private static final double FORM_ROW_SPACING = 6;
  private static final double FORM_ROW_PADDING = 8;
  private static final double CONTENT_PADDING = 8;
  private static final double SPRITE_ROW_SPACING = 8;
  private static final double BUTTON_BAR_SPACING = 8;
  private static final double DEFAULT_FRICTION = 0.5;
  private static final double LIST_WIDTH = 260;
  private static final double TAB_CONTENT_SPACING = 12;

  private final GameCatalogDraft draft;
  private final SpriteImportService spriteImportService;

  private ListView<String> terrainListView;

  TerrainCatalogTab(GameCatalogDraft draft, SpriteImportService spriteImportService) {
    this.draft = Objects.requireNonNull(draft);
    this.spriteImportService = Objects.requireNonNull(spriteImportService);
  }

  Tab build() {
    terrainListView = new ListView<>();
    terrainListView.setId("terrain-list");
    refresh();

    TextField idF = AuthoringValueHints.newTextField("catalog.terrain.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.terrain.spriteUrl");
    ColorPicker colorF = new ColorPicker(Color.WHITE);
    colorF.setPrefWidth(DEFAULT_WIDTH);
    spriteF.textProperty().addListener((obs, oldVal, newVal) ->
        colorF.setDisable(!newVal.isBlank()));
    TextField frictionF = AuthoringValueHints.newTextField("catalog.terrain.friction");

    Button add = new Button("Add");
    add.setOnAction(e -> {
      draft.terrainTypes().add(new TerrainInfo(
          idF.getText().strip(),
          nullIfBlank(spriteF.getText()),
          colorValue(spriteF, colorF),
          parseDouble(frictionF.getText(), DEFAULT_FRICTION)));
      colorF.setValue(Color.WHITE);
      clearFields(idF, spriteF, frictionF);
      refresh();
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> selectedIndex(terrainListView, draft.terrainTypes().size())
        .ifPresent(index -> {
          draft.terrainTypes().remove(index.intValue());
          refresh();
        }));

    return new Tab("Terrain", buildTabContent(terrainListView,
        formRow("ID", idF), spriteRow(spriteF),
        formRow("Color", colorF, FORM_ROW_WIDTH), formRow("Friction", frictionF),
        buttonBar(add, remove)));
  }

  void refresh() {
    if (terrainListView == null) {
      return;
    }
    terrainListView.getItems().setAll(draft.terrainTypes().stream()
        .map(t -> t.id() + "  [friction=" + t.friction() + "]")
        .toList());
  }

  private String colorValue(TextField spriteField, ColorPicker colorPicker) {
    if (!spriteField.getText().isBlank()) {
      return null;
    }
    Color color = colorPicker.getValue();
    return String.format("#%02x%02x%02x",
        (int) (color.getRed() * 255),
        (int) (color.getGreen() * 255),
        (int) (color.getBlue() * 255));
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

  private HBox formRow(String labelText, Node control, double labelWidth) {
    Label label = new Label(labelText + ":");
    label.setPrefWidth(labelWidth);
    return new HBox(BUTTON_BAR_SPACING, label, control);
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

  private String nullIfBlank(String value) {
    return value == null || value.isBlank() ? null : value.strip();
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

  private java.util.Optional<Integer> selectedIndex(ListView<String> list, int size) {
    int index = list.getSelectionModel().getSelectedIndex();
    return index >= 0 && index < size ? java.util.Optional.of(index) : java.util.Optional.empty();
  }
}
