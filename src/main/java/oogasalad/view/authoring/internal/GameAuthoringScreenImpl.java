package oogasalad.view.authoring.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.CheatTriggerInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.GameCatalogSaver;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.view.core.internal.AbstractAppScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Game catalog editor screen. Authors can define terrain types, static objects (pickups/hazards),
 * and movable components with their on-screen buttons. The catalog is saved to {@code game.json}
 * and then used to drive the level editor and vehicle builder.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 */
public class GameAuthoringScreenImpl extends AbstractAppScreen {

  private static final Logger LOG = LogManager.getLogger(GameAuthoringScreenImpl.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.GameAuthoring");
  private static final Path ASSETS_DIR = Path.of("data", "assets");
  private static final Path DEFAULT_GAMES_DIR = Path.of("data", "games");
  private static final Path DEFAULT_LEVELS_DIR = Path.of("data", "levels");
  private static final String GAME_FILE_NAME = "game.json";
  private static final String DEFAULT_CHEAT_TARGET_KIND = "world";

  private final Consumer<String> loadLevelHandler;
  private final Consumer<GameConfig> onGameConfigReady;
  private final FileChooserService fileChooserService;
  private final GameCatalogSaver catalogSaver;
  private final Path gamesDir;
  private final Path levelsDir;
  private final CheatAuthoringValidator cheatValidator = new CheatAuthoringValidator();

  private final List<TerrainInfo> terrainTypes = new ArrayList<>();
  private final List<StaticComponentInfo> staticComponents = new ArrayList<>();
  private final List<ComponentInfo> components = new ArrayList<>();
  private final List<CheatInfo> cheats = new ArrayList<>();
  private TextField gameIdField;
  private TextField mainCharacterIdField;
  private ListView<String> terrainListView;
  private ListView<String> staticListView;
  private ListView<String> componentListView;
  private ListView<String> cheatListView;
  private String saveFilePath = "data/games/untitled/game.json";

  public GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady) {
    this(onOutcome, loadLevelHandler, onGameConfigReady, new GameCatalogSaver(),
        DEFAULT_GAMES_DIR, DEFAULT_LEVELS_DIR, new FileChooserService());
  }

  GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady, GameCatalogSaver catalogSaver, Path gamesDir,
      Path levelsDir, FileChooserService fileChooserService) {
    super(onOutcome);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.onGameConfigReady = onGameConfigReady != null ? onGameConfigReady : cfg -> {
    };
    this.catalogSaver = catalogSaver;
    this.gamesDir = gamesDir;
    this.levelsDir = levelsDir;
    this.fileChooserService = fileChooserService;
  }

  public void loadCatalogFromPath(String path) {
    try {
      GameConfig config = new GameConfigImpl(path);
      saveFilePath = path;
      gameIdField.setText(config.gameId());
      mainCharacterIdField.setText(config.mainCharacterId());
      terrainTypes.clear();
      terrainTypes.addAll(config.terrainTypes());
      staticComponents.clear();
      staticComponents.addAll(config.staticComponents());
      components.clear();
      components.addAll(config.components());
      cheats.clear();
      cheats.addAll(config.cheats());
      refreshCatalogLists();
    } catch (Exception e) {
      LOG.warn("Could not load game catalog from {}", path, e);
      new AuthoringAlertService().showError("Load Error", e.getMessage());
    }
  }

  public void resetForNewCatalog() {
    saveFilePath = "data/games/untitled/game.json";
    Optional.ofNullable(gameIdField).ifPresent(TextField::clear);
    Optional.ofNullable(mainCharacterIdField).ifPresent(TextField::clear);
    terrainTypes.clear();
    staticComponents.clear();
    components.clear();
    cheats.clear();
    refreshCatalogLists();
  }

  @Override
  protected Parent createRoot() {
    BorderPane layout = buildRootPane();
    layout.setTop(buildHeader());
    layout.setCenter(buildCatalogEditor());
    layout.setBottom(buildFooter());
    return layout;
  }

  private BorderPane buildRootPane() {
    BorderPane pane = new BorderPane();
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/authoring-entry.css").toExternalForm());
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/game-authoring.css").toExternalForm());
    pane.getStyleClass().addAll("screen-root", "game-authoring-root");
    pane.setPadding(new Insets(d("rootPadding")));
    return pane;
  }

  private VBox buildHeader() {
    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));
    title.getStyleClass().add("authoring-entry-title");

    gameIdField = new TextField();
    gameIdField.setPromptText("game id, e.g. badpiggies");
    gameIdField.setPrefWidth(200);

    mainCharacterIdField = new TextField();
    mainCharacterIdField.setPromptText("main character id");
    mainCharacterIdField.setPrefWidth(200);

    HBox metaRow = new HBox(12,
        new Label("Game ID:"), gameIdField,
        new Label("Main Character:"), mainCharacterIdField);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    Button loadCatalogButton = new Button(s("loadCatalogButtonText"));
    loadCatalogButton.getStyleClass().add("authoring-entry-action");
    loadCatalogButton.setOnAction(e -> handleLoadCatalog(loadCatalogButton));

    HBox topRow = new HBox(16, metaRow, loadCatalogButton);
    topRow.setAlignment(Pos.CENTER_LEFT);

    VBox header = new VBox(8, title, topRow);
    header.getStyleClass().add("authoring-entry-center");
    return header;
  }

  private TabPane buildCatalogEditor() {
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getTabs().addAll(
        buildTerrainTab(),
        buildStaticTab(),
        buildComponentsTab(),
        buildCheatsTab()
    );
    VBox.setVgrow(tabs, Priority.ALWAYS);
    return tabs;
  }

  private Tab buildTerrainTab() {
    terrainListView = new ListView<>();
    terrainListView.setId("terrain-list");
    refreshTerrainList(terrainListView);

    TextField idF = field("id");
    TextField spriteF = field("spriteUrl");
    TextField colorF = field("visualColor, e.g. #8B4513");
    TextField frictionF = field("friction, e.g. 0.6");

    Button add = new Button("Add");
    add.setOnAction(e -> {
      terrainTypes.add(new TerrainInfo(
          idF.getText().strip(),
          nullIfBlank(spriteF.getText()),
          nullIfBlank(colorF.getText()),
          parseDouble(frictionF.getText(), 0.5)));
      refreshTerrainList(terrainListView);
      clearFields(idF, spriteF, colorF, frictionF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = terrainListView.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        terrainTypes.remove(idx);
        refreshTerrainList(terrainListView);
      }
    });

    VBox form = tabForm(
        List.of(
            formRow("ID", idF), spriteRow(spriteF),
            formRow("Color", colorF), formRow("Friction", frictionF)),
        buttonBar(add, remove));

    return new Tab("Terrain", buildTabContent(terrainListView, form));
  }

  private void refreshTerrainList(ListView<String> list) {
    if (terrainTypes == null) {
      return;
    }
    list.getItems().setAll(terrainTypes.stream()
        .map(t -> t.id() + "  [friction=" + t.friction() + "]").toList());
  }

  private Tab buildStaticTab() {
    staticListView = new ListView<>();
    staticListView.setId("static-component-list");
    refreshStaticList(staticListView);

    TextField idF = field("id");
    TextField spriteF = field("spriteUrl");
    TextField massF = field("mass, e.g. 0");
    TextField actionTypeF = field("action type: heal / damage / score / finish");
    TextField actionAmountF = field("action amount, e.g. 25");

    Button add = new Button("Add");
    add.setOnAction(e -> {
      String aType = actionTypeF.getText().strip();
      CollisionAction action = aType.isBlank() ? null
          : new CollisionAction(aType, parseDouble(actionAmountF.getText(), 0));
      staticComponents.add(new StaticComponentInfo(
          idF.getText().strip(),
          spriteF.getText().strip(),
          parseDouble(massF.getText(), 0),
          action));
      refreshStaticList(staticListView);
      clearFields(idF, spriteF, massF, actionTypeF, actionAmountF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = staticListView.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        staticComponents.remove(idx);
        refreshStaticList(staticListView);
      }
    });

    VBox form = tabForm(
        List.of(
            formRow("ID", idF), spriteRow(spriteF),
            formRow("Mass", massF), formRow("Action Type", actionTypeF),
            formRow("Action Amount", actionAmountF)),
        buttonBar(add, remove));

    return new Tab("Static Objects", buildTabContent(staticListView, form));
  }

  private void refreshStaticList(ListView<String> list) {
    if (staticComponents == null) {
      return;
    }
    list.getItems().setAll(staticComponents.stream()
        .map(s -> s.id() + (s.onCollisionAction() != null
            ? "  [" + s.onCollisionAction().type() + "]" : "  [inert]")).toList());
  }

  private Tab buildComponentsTab() {
    componentListView = new ListView<>();
    componentListView.setId("component-list");
    refreshComponentList(componentListView);

    TextField idF = field("id");
    TextField spriteF = field("spriteUrl");
    TextField massF = field("mass, e.g. 1.0");
    TextField btnNameF = field("button name, e.g. go right");
    TextField btnDxF = field("direction x, e.g. 1");
    TextField btnDyF = field("direction y, e.g. 0");

    List<ButtonInfo> pendingButtons = new ArrayList<>();
    ListView<String> btnList = new ListView<>();
    btnList.setPrefHeight(80);

    Button addBtn = new Button("Add Button");
    addBtn.setOnAction(e -> {
      String name = btnNameF.getText().strip();
      if (!name.isBlank()) {
        pendingButtons.add(new ButtonInfo(name,
            parseDouble(btnDxF.getText(), 0),
            parseDouble(btnDyF.getText(), 0)));
        btnList.getItems().add(name + " (" + btnDxF.getText() + "," + btnDyF.getText() + ")");
        clearFields(btnNameF, btnDxF, btnDyF);
      }
    });

    Button add = new Button("Add Component");
    add.setOnAction(e -> {
      components.add(new ComponentInfo(
          idF.getText().strip(),
          spriteF.getText().strip(),
          parseDouble(massF.getText(), 1.0),
          new ArrayList<>(pendingButtons)));
      pendingButtons.clear();
      btnList.getItems().clear();
      refreshComponentList(componentListView);
      clearFields(idF, spriteF, massF);
    });

    Button useAsMainCharacter = new Button("Use as Main Character");
    useAsMainCharacter.setOnAction(e -> selectedComponentId(componentListView)
        .ifPresent(mainCharacterIdField::setText));

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = componentListView.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        String removedId = components.get(idx).id();
        components.remove(idx);
        if (removedId.equals(mainCharacterIdField.getText().strip())) {
          mainCharacterIdField.clear();
        }
        refreshComponentList(componentListView);
      }
    });

    VBox form = tabForm(
        List.of(
            formRow("ID", idF), spriteRow(spriteF), formRow("Mass", massF),
            new Label("Buttons:"), btnList,
            formRow("Button Name", btnNameF), formRow("Dir X", btnDxF),
            formRow("Dir Y", btnDyF)),
        buttonBar(addBtn),
        buttonBar(add, useAsMainCharacter, remove));

    return new Tab("Components", buildTabContent(componentListView, form));
  }

  private Optional<String> selectedComponentId(ListView<String> list) {
    int idx = list.getSelectionModel().getSelectedIndex();
    if (idx < 0 || idx >= components.size()) {
      return Optional.empty();
    }
    return Optional.of(components.get(idx).id());
  }

  private void refreshComponentList(ListView<String> list) {
    if (components == null) {
      return;
    }
    list.getItems().setAll(components.stream()
        .map(c -> c.id() + "  [" + c.buttons().size() + " buttons]").toList());
  }

  private Tab buildCheatsTab() {
    cheatListView = new ListView<>();
    cheatListView.setId("cheat-list");
    refreshCheatList();

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
        mainCharacterIdField.getText().strip(), components, staticComponents, terrainTypes)
        .show());

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

  private void addPendingCheatEffect(List<CheatEffectInfo> pendingEffects,
      ListView<String> effectList, TextField effectTypeF, TextField targetKindF,
      TextField targetIdF, TextField parametersF) {
    try {
      CheatEffectInfo effect = new CheatEffectInfo(
          requiredText(effectTypeF, "Effect Type"),
          cheatTargetFrom(targetKindF, targetIdF),
          parseParameterMap(parametersF.getText()));
      cheatValidator.validateEffect(effect, staticComponents);
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
      cheats.add(cheat);
      pendingEffects.clear();
      refreshPendingEffectList(pendingEffects, effectList);
      refreshCheatList();
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
    cheatValidator.validateCheat(cheat, staticComponents);
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
    selectedIndex(cheatListView, currentCheats().size()).ifPresent(index -> {
      cheats.remove(index.intValue());
      refreshCheatList();
    });
  }

  private Optional<Integer> selectedIndex(ListView<String> list, int size) {
    int index = list.getSelectionModel().getSelectedIndex();
    return index >= 0 && index < size ? Optional.of(index) : Optional.empty();
  }

  private void refreshCheatList() {
    Optional.ofNullable(cheatListView)
        .ifPresent(list -> list.getItems().setAll(currentCheats().stream()
            .map(this::formatCheat)
            .toList()));
  }

  private void refreshCatalogLists() {
    Optional.ofNullable(terrainListView).ifPresent(this::refreshTerrainList);
    Optional.ofNullable(staticListView).ifPresent(this::refreshStaticList);
    Optional.ofNullable(componentListView).ifPresent(this::refreshComponentList);
    refreshCheatList();
  }

  private void refreshPendingEffectList(List<CheatEffectInfo> pendingEffects,
      ListView<String> effectList) {
    effectList.getItems().setAll(pendingEffects.stream()
        .map(this::formatEffect)
        .toList());
  }

  private List<CheatInfo> currentCheats() {
    return Optional.ofNullable(cheats).orElse(List.of());
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
    new AuthoringAlertService().showError("Cheat Error", e.getMessage());
  }

  private HBox buildFooter() {
    Button saveButton = new Button(s("saveGameButtonText"));
    saveButton.getStyleClass().add("authoring-entry-action");
    saveButton.setOnAction(e -> handleSaveCatalog());

    Button createLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createLevelButton);
    createLevelButton.setOnAction(e -> navigateToLevelEditor());

    Button loadLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadLevelButton);
    loadLevelButton.setOnAction(e -> handleLoadLevel(loadLevelButton));

    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_AUTHORING_ENTRY));

    HBox footer = new HBox(12, backButton, saveButton, createLevelButton, loadLevelButton);
    footer.setAlignment(Pos.CENTER_LEFT);
    footer.setPadding(new Insets(d("buttonRegionPadding"), 0, 0, 0));
    footer.getStyleClass().add("authoring-entry-button-row");
    return footer;
  }

  private void handleSaveCatalog() {
    doSave();
  }

  private void doSave() {
    try {
      String gameId = gameIdField.getText().strip();
      Path catalogPath = saveCatalogForGameId(gameId);
      saveFilePath = catalogPath.toString();
      new AuthoringAlertService().showInfo("Saved", "Game catalog saved to " + saveFilePath);
    } catch (IOException | RuntimeException e) {
      LOG.warn("Could not save game catalog: {}", e.getMessage());
      new AuthoringAlertService().showError("Save Error", e.getMessage());
    }
  }

  private Path saveCatalogForGameId(String gameId) throws IOException {
    if (gameId.isBlank()) {
      throw new IllegalArgumentException("Game ID is required before saving a game catalog.");
    }
    Path catalogPath = catalogPathFor(gameId);
    Files.createDirectories(catalogPath.getParent());
    Files.createDirectories(levelsDir.resolve(gameId));
    String mainCharacterId = mainCharacterIdField.getText().strip();
    validateMainCharacterId(mainCharacterId);
    cheatValidator.validateCheats(cheats, staticComponents);
    catalogSaver.save(catalogPath.toString(),
        gameId,
        mainCharacterId,
        components, staticComponents, terrainTypes, cheats);
    return catalogPath;
  }

  private void validateMainCharacterId(String mainCharacterId) {
    if (mainCharacterId.isBlank()) {
      throw new IllegalArgumentException("Main Character is required before saving a game catalog.");
    }
    boolean exists = components.stream().anyMatch(component -> mainCharacterId.equals(component.id()));
    if (!exists) {
      throw new IllegalArgumentException(
          "Main Character must be one of the vehicle components.");
    }
  }

  private Path catalogPathFor(String gameId) {
    return gamesDir.resolve(gameId).resolve(GAME_FILE_NAME);
  }

  private GameConfig saveAndLoadCurrentConfig() {
    try {
      String gameId = gameIdField.getText().strip();
      Path catalogPath = saveCatalogForGameId(gameId);
      saveFilePath = catalogPath.toString();
      return new GameConfigImpl(saveFilePath);
    } catch (IOException | RuntimeException e) {
      LOG.warn("Could not prepare current game config: {}", e.getMessage());
      new AuthoringAlertService().showError("Save Error", e.getMessage());
      return GameConfigImpl.empty();
    }
  }

  private void handleLoadCatalog(Button source) {
    fileChooserService.chooseLoadFile(source.getScene().getWindow())
        .ifPresent(file -> loadCatalogFromPath(file.getAbsolutePath()));
  }

  private void handleLoadLevel(Button source) {
    fileChooserService.chooseLoadFile(source.getScene().getWindow())
        .ifPresent(file -> {
          loadLevelHandler.accept(file.getAbsolutePath());
          navigateToLevelEditor();
        });
  }

  private void navigateToLevelEditor() {
    onGameConfigReady.accept(buildCurrentConfig());
    onOutcome.accept(Outcome.OPEN_AUTHORING);
  }

  private GameConfig buildCurrentConfig() {
    return saveAndLoadCurrentConfig();
  }

  private Node buildTabContent(ListView<String> list, VBox form) {
    list.setPrefWidth(260);
    list.setMaxHeight(Double.MAX_VALUE);
    form.setPadding(new Insets(8));
    form.setMaxHeight(Double.MAX_VALUE);
    HBox content = new HBox(12, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(8, 8, 16, 8));
    return content;
  }

  private VBox tabForm(List<Node> fields, Node... actionRows) {
    VBox form = new VBox(6);
    form.getChildren().addAll(fields);
    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);
    VBox actions = new VBox(8, actionRows);
    actions.setAlignment(Pos.BOTTOM_RIGHT);
    actions.getStyleClass().add("game-authoring-action-area");
    form.getChildren().addAll(spacer, actions);
    return form;
  }

  private HBox spriteRow(TextField spriteField) {
    Button browse = new Button("Browse…");
    browse.setOnAction(e -> browseSprite(browse, spriteField));
    Label lbl = new Label("Sprite:");
    lbl.setPrefWidth(120);
    HBox.setHgrow(spriteField, Priority.ALWAYS);
    return new HBox(8, lbl, spriteField, browse);
  }

  private void browseSprite(Node owner, TextField target) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Choose Sprite Image");
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
    File selected = chooser.showOpenDialog(owner.getScene().getWindow());
    if (selected == null) {
      return;
    }
    try {
      Files.createDirectories(ASSETS_DIR);
      Path dest = ASSETS_DIR.resolve(selected.getName());
      Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
      target.setText(dest.toString());
      LOG.info("Copied sprite to {}", dest);
    } catch (IOException ex) {
      new AuthoringAlertService().showError("Sprite Import Error", ex.getMessage());
    }
  }

  private HBox formRow(String label, TextField field) {
    Label lbl = new Label(label + ":");
    lbl.setPrefWidth(120);
    return new HBox(8, lbl, field);
  }

  private HBox buttonBar(Button... buttons) {
    HBox bar = new HBox(8, buttons);
    bar.setAlignment(Pos.CENTER_RIGHT);
    bar.getStyleClass().add("game-authoring-action-row");
    return bar;
  }

  private HBox inlineButtonBar(Button... buttons) {
    HBox bar = new HBox(8, buttons);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.getStyleClass().add("game-authoring-inline-action-row");
    return bar;
  }

  private TextField field(String prompt) {
    TextField f = new TextField();
    f.setPromptText(prompt);
    f.setPrefWidth(220);
    return f;
  }

  private TextField field(String prompt, String id) {
    TextField f = field(prompt);
    f.setId(id);
    return f;
  }

  private void clearFields(TextField... fields) {
    for (TextField f : fields) {
      f.clear();
    }
  }

  private String nullIfBlank(String s) {
    return (s == null || s.isBlank()) ? null : s.strip();
  }

  private double parseDouble(String text, double fallback) {
    try {
      return Double.parseDouble(text.strip());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(d("actionFontSize")));
    button.setPrefSize(d("actionButtonWidth"), d("actionButtonHeight"));
    button.setWrapText(true);
    button.getStyleClass().add("authoring-entry-action");
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
