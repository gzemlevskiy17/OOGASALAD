package oogasalad.view.authoring.internal.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.internal.BuiltInVehicleCatalog;
import oogasalad.view.authoring.internal.common.AuthoringAlertService;
import oogasalad.view.authoring.internal.common.AuthoringValueHints;

/**
 * Vehicle component editor tab for the game catalog authoring screen.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class ComponentCatalogTab {

  private static final double DEFAULT_WIDTH = 220;
  private static final double FORM_ROW_WIDTH = 120;
  private static final double FORM_ROW_SPACING = 6;
  private static final double FORM_ROW_PADDING = 8;
  private static final double CONTENT_PADDING = 8;
  private static final double SPRITE_ROW_SPACING = 8;
  private static final double BUTTON_BAR_SPACING = 8;
  private static final double BUTTON_LIST_HEIGHT = 80;
  private static final double FORM_SPACING = 6;
  private static final double DEFAULT_MASS = 1.0;
  private static final double LIST_WIDTH = 260;
  private static final double TAB_CONTENT_SPACING = 12;

  private final GameCatalogDraft draft;
  private final SpriteImportService spriteImportService;
  private final AuthoringAlertService alertService;
  private final Supplier<String> mainCharacterIdSupplier;
  private final Consumer<String> mainCharacterIdSetter;

  private ListView<String> componentListView;
  private ListView<String> builtinPickerListView;

  ComponentCatalogTab(GameCatalogDraft draft, SpriteImportService spriteImportService,
      AuthoringAlertService alertService, Supplier<String> mainCharacterIdSupplier,
      Consumer<String> mainCharacterIdSetter) {
    this.draft = Objects.requireNonNull(draft);
    this.spriteImportService = Objects.requireNonNull(spriteImportService);
    this.alertService = Objects.requireNonNull(alertService);
    this.mainCharacterIdSupplier = Objects.requireNonNull(mainCharacterIdSupplier);
    this.mainCharacterIdSetter = Objects.requireNonNull(mainCharacterIdSetter);
  }

  Tab build() {
    componentListView = new ListView<>();
    componentListView.setId("component-list");
    refresh();

    TextField idF = AuthoringValueHints.newTextField("catalog.component.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.component.spriteUrl");
    TextField massF = AuthoringValueHints.newTextField("catalog.component.mass");
    TextField btnNameF = AuthoringValueHints.newTextField("catalog.component.buttonLabel");
    TextField btnDxF = AuthoringValueHints.newTextField("catalog.component.directionX");
    TextField btnDyF = AuthoringValueHints.newTextField("catalog.component.directionY");

    VBox presetBlock = buildPresetBlock();
    List<ButtonInfo> pendingButtons = new ArrayList<>();
    ListView<String> btnList = new ListView<>();
    btnList.setPrefHeight(BUTTON_LIST_HEIGHT);

    Button addBtn = new Button("Add Button");
    addBtn.setOnAction(e -> addPendingButton(pendingButtons, btnList, btnNameF, btnDxF, btnDyF));

    Button add = new Button("Add Component");
    add.setOnAction(e -> {
      draft.components().add(new ComponentInfo(
          idF.getText().strip(),
          spriteF.getText().strip(),
          parseDouble(massF.getText(), DEFAULT_MASS),
          new ArrayList<>(pendingButtons)));
      pendingButtons.clear();
      btnList.getItems().clear();
      refresh();
      clearFields(idF, spriteF, massF);
    });

    Button useAsMainCharacter = new Button("Use as Main Character");
    useAsMainCharacter.setOnAction(e -> selectedComponentId()
        .ifPresent(id -> {
          mainCharacterIdSetter.accept(id);
          alertService.showInfo("Main Character Set", "Main character set to: " + id);
        }));

    Button remove = new Button("Remove");
    remove.setOnAction(e -> selectedIndex(componentListView, draft.components().size())
        .ifPresent(index -> removeComponent(index)));

    VBox form = tabForm(
        List.of(
            presetBlock,
            new Label("Custom component"),
            formRow("ID", idF), spriteRow(spriteF), formRow("Mass", massF),
            new Label("Buttons:"), btnList,
            formRow("Button Name", btnNameF), formRow("Dir X", btnDxF),
            formRow("Dir Y", btnDyF)),
        buttonBar(addBtn),
        buttonBar(add, useAsMainCharacter, remove));

    return new Tab("Components", buildTabContent(componentListView, form));
  }

  void refresh() {
    if (componentListView != null) {
      componentListView.getItems().setAll(draft.components().stream()
          .map(component -> component.id() + "  [" + component.buttons().size() + " buttons]")
          .toList());
    }
    if (builtinPickerListView != null) {
      builtinPickerListView.getItems().setAll(
          BuiltInVehicleCatalog.idsNotYetInCatalog(draft.components()));
    }
  }

  private VBox buildPresetBlock() {
    builtinPickerListView = new ListView<>();
    builtinPickerListView.setPrefHeight(100);
    builtinPickerListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    Label presetLabel = new Label("Preset parts (add to catalog):");
    Button addSelectedPresets = new Button("Add selected presets");
    addSelectedPresets.setOnAction(e -> {
      for (String id : new ArrayList<>(builtinPickerListView.getSelectionModel()
          .getSelectedItems())) {
        BuiltInVehicleCatalog.templateById(id)
            .ifPresent(template -> addIfMissing(template));
      }
      refresh();
    });
    refresh();
    return new VBox(FORM_SPACING, presetLabel, builtinPickerListView,
        new HBox(BUTTON_BAR_SPACING, addSelectedPresets));
  }

  private void addPendingButton(List<ButtonInfo> pendingButtons, ListView<String> btnList,
      TextField btnNameF, TextField btnDxF, TextField btnDyF) {
    String name = btnNameF.getText().strip();
    if (name.isBlank()) {
      return;
    }
    pendingButtons.add(new ButtonInfo(name,
        parseDouble(btnDxF.getText(), 0),
        parseDouble(btnDyF.getText(), 0)));
    btnList.getItems().add(name + " (" + btnDxF.getText() + "," + btnDyF.getText() + ")");
    clearFields(btnNameF, btnDxF, btnDyF);
  }

  private void addIfMissing(ComponentInfo template) {
    if (draft.components().stream().noneMatch(component -> component.id().equals(template.id()))) {
      draft.components().add(template);
    }
  }

  private void removeComponent(Integer index) {
    String removedId = draft.components().get(index).id();
    draft.components().remove(index.intValue());
    if (removedId.equals(mainCharacterIdSupplier.get().strip())) {
      mainCharacterIdSetter.accept("");
    }
    refresh();
  }

  private Optional<String> selectedComponentId() {
    int index = componentListView.getSelectionModel().getSelectedIndex();
    if (index < 0 || index >= draft.components().size()) {
      return Optional.empty();
    }
    return Optional.of(draft.components().get(index).id());
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

  private HBox buttonBar(Button... buttons) {
    HBox bar = new HBox(BUTTON_BAR_SPACING, buttons);
    bar.setAlignment(Pos.CENTER_RIGHT);
    bar.getStyleClass().add("game-authoring-action-row");
    return bar;
  }

  private Node buildTabContent(ListView<String> list, VBox form) {
    list.setPrefWidth(LIST_WIDTH);
    list.setMaxHeight(Double.MAX_VALUE);
    form.setPadding(new Insets(FORM_ROW_PADDING));
    form.setMaxHeight(Double.MAX_VALUE);
    HBox content = new HBox(TAB_CONTENT_SPACING, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(CONTENT_PADDING, CONTENT_PADDING, 16, CONTENT_PADDING));
    return content;
  }

  private VBox tabForm(List<Node> fields, Node... actionRows) {
    VBox form = new VBox(FORM_ROW_SPACING);
    form.getChildren().addAll(fields);
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    VBox actions = new VBox(BUTTON_BAR_SPACING, actionRows);
    actions.setAlignment(Pos.BOTTOM_RIGHT);
    actions.getStyleClass().add("game-authoring-action-area");
    form.getChildren().addAll(spacer, actions);
    return form;
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
