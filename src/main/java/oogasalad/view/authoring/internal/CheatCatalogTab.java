package oogasalad.view.authoring.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Cheat key editor tab for the game catalog authoring screen.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class CheatCatalogTab {

  private static final Logger LOG = LogManager.getLogger(CheatCatalogTab.class);
  private static final double DEFAULT_WIDTH = 220;
  private static final double FORM_ROW_WIDTH = 120;
  private static final double FORM_ROW_SPACING = 6;
  private static final double FORM_ROW_PADDING = 8;
  private static final double CONTENT_PADDING = 8;
  private static final double BUTTON_BAR_SPACING = 8;
  private static final double LIST_WIDTH = 260;
  private static final double TAB_CONTENT_SPACING = 12;
  private static final String DEFAULT_CHEAT_TARGET_KIND = "world";

  private final GameCatalogDraft draft;
  private final CheatAuthoringValidator cheatValidator;
  private final AuthoringAlertService alertService;
  private final Supplier<String> mainCharacterIdSupplier;

  private ListView<String> cheatListView;

  CheatCatalogTab(GameCatalogDraft draft, CheatAuthoringValidator cheatValidator,
      AuthoringAlertService alertService, Supplier<String> mainCharacterIdSupplier) {
    this.draft = Objects.requireNonNull(draft);
    this.cheatValidator = Objects.requireNonNull(cheatValidator);
    this.alertService = Objects.requireNonNull(alertService);
    this.mainCharacterIdSupplier = Objects.requireNonNull(mainCharacterIdSupplier);
  }

  Tab build() {
    cheatListView = new ListView<>();
    cheatListView.setId("cheat-list");
    refresh();

    TextField idF = field("cheat id, e.g. spawn_heart", "cheat-id-field");
    TextField nameF = field("display name, e.g. Spawn Heart", "cheat-name-field");
    TextField keysF = field("key chord, e.g. SHIFT,P", "cheat-keys-field");
    CheckBox enabledBox = new CheckBox("Enabled");
    enabledBox.setId("cheat-enabled-checkbox");
    enabledBox.setSelected(true);

    TextField effectTypeF = field("effect type, e.g. spawnPowerup", "cheat-effect-type-field");
    TextField targetKindF = field("target kind, e.g. world", "cheat-target-kind-field");
    TextField targetIdF = field("target id, optional", "cheat-target-id-field");
    TextField parametersF =
        field("parameters, e.g. componentId=heart,x=400,y=600", "cheat-parameters-field");

    List<CheatEffectInfo> pendingEffects = new ArrayList<>();
    ListView<String> effectList = new ListView<>();
    effectList.setId("cheat-effect-list");
    effectList.setMinHeight(120);
    effectList.setPrefHeight(150);

    Button addEffect = new Button("Add Effect");
    addEffect.setId("add-cheat-effect-button");
    addEffect.setOnAction(e -> addPendingCheatEffect(pendingEffects, effectList,
        effectTypeF, targetKindF, targetIdF, parametersF));

    Button removeEffect = new Button("Remove Effect");
    removeEffect.setId("remove-cheat-effect-button");
    removeEffect.setOnAction(e -> removeSelectedPendingEffect(pendingEffects, effectList));

    Button addCheat = new Button("Add Cheat");
    addCheat.setId("add-cheat-button");
    addCheat.setOnAction(e -> addAuthoredCheat(
        idF, nameF, keysF, enabledBox, pendingEffects, effectList));

    Button removeCheat = new Button("Remove Cheat");
    removeCheat.setId("remove-cheat-button");
    removeCheat.setOnAction(e -> removeSelectedCheat());

    Button helpButton = new Button("Cheat Help");
    helpButton.setId("cheat-help-button");
    helpButton.setOnAction(e -> new CheatAuthoringHelpDialog(
        mainCharacterIdSupplier.get(), draft.components(), draft.staticComponents(),
        draft.terrainTypes()).show());

    VBox form = tabForm(
        List.of(
            formRow("ID", idF), formRow("Name", nameF), formRow("Keys", keysF),
            enabledBox,
            formRow("Effect Type", effectTypeF), formRow("Target Kind", targetKindF),
            formRow("Target ID", targetIdF), formRow("Parameters", parametersF),
            inlineButtonBar(addEffect, removeEffect),
            new Label("Pending Effects:"), effectList),
        buttonBar(addCheat, removeCheat, helpButton));

    return new Tab("Cheats", buildTabContent(cheatListView, form));
  }

  void refresh() {
    if (cheatListView == null) {
      return;
    }
    cheatListView.getItems().setAll(draft.cheats().stream()
        .map(this::formatCheat)
        .toList());
  }

  private void addPendingCheatEffect(List<CheatEffectInfo> pendingEffects,
      ListView<String> effectList, TextField effectTypeF, TextField targetKindF,
      TextField targetIdF, TextField parametersF) {
    try {
      CheatEffectInfo effect = new CheatEffectInfo(
          requiredText(effectTypeF, "Effect Type"),
          cheatTargetFrom(targetKindF, targetIdF),
          parseParameterMap(parametersF.getText()));
      cheatValidator.validateEffect(effect, draft.staticComponents());
      pendingEffects.add(effect);
      refreshPendingEffectList(pendingEffects, effectList);
      clearFields(effectTypeF, targetKindF, targetIdF, parametersF);
    } catch (CheatAuthoringException e) {
      showCheatAuthoringError(e);
    }
  }

  private void addAuthoredCheat(TextField idF, TextField nameF, TextField keysF,
      CheckBox enabledBox, List<CheatEffectInfo> pendingEffects, ListView<String> effectList) {
    try {
      CheatInfo cheat = cheatFrom(idF, nameF, keysF, enabledBox, pendingEffects);
      draft.cheats().add(cheat);
      pendingEffects.clear();
      refreshPendingEffectList(pendingEffects, effectList);
      refresh();
      clearFields(idF, nameF, keysF);
      enabledBox.setSelected(true);
    } catch (CheatAuthoringException e) {
      showCheatAuthoringError(e);
    }
  }

  private CheatInfo cheatFrom(TextField idF, TextField nameF, TextField keysF,
      CheckBox enabledBox, List<CheatEffectInfo> pendingEffects) {
    String id = requiredText(idF, "Cheat ID");
    List<String> keys = parseKeyChord(keysF.getText());
    requireNonEmpty(keys, "Cheat key chord must include at least one key.");
    requireNonEmpty(pendingEffects, "Cheat must include at least one effect.");
    CheatInfo cheat = new CheatInfo(
        id,
        Optional.of(nameF.getText().strip()).filter(value -> !value.isBlank()).orElse(id),
        enabledBox.isSelected(),
        new CheatTriggerInfo(keys),
        List.copyOf(pendingEffects));
    cheatValidator.validateCheat(cheat, draft.staticComponents());
    return cheat;
  }

  private CheatTargetInfo cheatTargetFrom(TextField targetKindF, TextField targetIdF) {
    String kind = Optional.of(targetKindF.getText().strip())
        .filter(value -> !value.isBlank())
        .orElse(DEFAULT_CHEAT_TARGET_KIND);
    return new CheatTargetInfo(kind, targetIdF.getText().strip());
  }

  private List<String> parseKeyChord(String rawKeys) {
    return Arrays.stream(rawKeys.split(","))
        .map(String::strip)
        .filter(key -> !key.isBlank())
        .toList();
  }

  private Map<String, String> parseParameterMap(String rawParameters) {
    Map<String, String> parameters = new LinkedHashMap<>();
    Arrays.stream(rawParameters.split(","))
        .map(String::strip)
        .filter(parameter -> !parameter.isBlank())
        .forEach(parameter -> addParameter(parameters, parameter));
    return parameters;
  }

  private void addParameter(Map<String, String> parameters, String rawParameter) {
    String[] pair = rawParameter.split("=", 2);
    if (pair.length != 2 || pair[0].isBlank()) {
      throw new CheatAuthoringException(
          "Cheat parameters must use key=value pairs separated by commas.");
    }
    parameters.put(pair[0].strip(), pair[1].strip());
  }

  private void requireNonEmpty(List<?> values, String message) {
    if (values.isEmpty()) {
      throw new CheatAuthoringException(message);
    }
  }

  private String requiredText(TextField field, String label) {
    return Optional.of(field.getText().strip())
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new CheatAuthoringException(label + " is required."));
  }

  private void removeSelectedPendingEffect(List<CheatEffectInfo> pendingEffects,
      ListView<String> effectList) {
    selectedIndex(effectList, pendingEffects.size()).ifPresent(index -> {
      pendingEffects.remove(index.intValue());
      refreshPendingEffectList(pendingEffects, effectList);
    });
  }

  private void removeSelectedCheat() {
    selectedIndex(cheatListView, draft.cheats().size()).ifPresent(index -> {
      draft.cheats().remove(index.intValue());
      refresh();
    });
  }

  private void refreshPendingEffectList(List<CheatEffectInfo> pendingEffects,
      ListView<String> effectList) {
    effectList.getItems().setAll(pendingEffects.stream()
        .map(this::formatEffect)
        .toList());
  }

  private String formatCheat(CheatInfo cheat) {
    return cheat.id() + "  [" + String.join("+", cheat.trigger().keys()) + "]  "
        + cheat.effects().size() + " effect(s)";
  }

  private String formatEffect(CheatEffectInfo effect) {
    CheatTargetInfo target = Optional.ofNullable(effect.target())
        .orElse(new CheatTargetInfo(DEFAULT_CHEAT_TARGET_KIND, ""));
    return effect.type() + " -> " + target.kind() + targetSuffix(target)
        + " " + effect.parameters();
  }

  private String targetSuffix(CheatTargetInfo target) {
    return Optional.ofNullable(target.id())
        .filter(value -> !value.isBlank())
        .map(value -> ":" + value)
        .orElse("");
  }

  private void showCheatAuthoringError(CheatAuthoringException e) {
    LOG.warn("Invalid cheat authoring input: {}", e.getMessage());
    alertService.showError("Cheat Error", e.getMessage());
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

  private HBox inlineButtonBar(Button... buttons) {
    HBox bar = new HBox(BUTTON_BAR_SPACING, buttons);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.getStyleClass().add("game-authoring-inline-action-row");
    return bar;
  }

  private TextField field(String prompt, String id) {
    TextField field = new TextField();
    field.setPromptText(prompt);
    field.setPrefWidth(DEFAULT_WIDTH);
    field.setId(id);
    return field;
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
