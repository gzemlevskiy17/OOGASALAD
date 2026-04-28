package oogasalad.view.authoring.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.BuiltInVehicleCatalog;
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
 * @author Iurii Beliaev
 */
public class GameAuthoringScreenImpl extends AbstractAppScreen {

  private static final Logger LOG = LogManager.getLogger(GameAuthoringScreenImpl.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.GameAuthoring");
  private static final Path ASSETS_DIR = Path.of("data", "assets");
  private static final Path GAMES_DIR = Path.of("data", "games");
  private static final Path LEVELS_DIR = Path.of("data", "levels");
  private static final String GAME_FILE_NAME = "game.json";

  private final Consumer<String> loadLevelHandler;
  private final Consumer<GameConfig> onGameConfigReady;
  private final FileChooserService fileChooserService;
  private final GameCatalogSaver catalogSaver = new GameCatalogSaver();

  private final List<TerrainInfo> terrainTypes = new ArrayList<>();
  private final List<StaticComponentInfo> staticComponents = new ArrayList<>();
  private final List<ComponentInfo> components = new ArrayList<>();
  private TextField gameIdField;
  private TextField mainCharacterIdField;
  private String saveFilePath = "data/games/untitled/game.json";
  private ListView<String> terrainListView;
  private ListView<String> staticListView;
  private ListView<String> componentListView;
  private ListView<String> builtinPickerListView;

  public GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady) {
    super(onOutcome);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.onGameConfigReady = onGameConfigReady != null ? onGameConfigReady : cfg -> {
    };
    this.fileChooserService = new FileChooserService();
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
      refreshAllLists();
    } catch (Exception e) {
      new AuthoringAlertService().showError("Load Error", e.getMessage());
    }
  }

  private void refreshAllLists() {
    if (terrainListView != null) {
      refreshTerrainList(terrainListView);
    }
    if (staticListView != null) {
      refreshStaticList(staticListView);
    }
    if (componentListView != null) {
      refreshComponentList(componentListView);
    }
    refreshBuiltinPicker();
  }

  private void refreshBuiltinPicker() {
    if (builtinPickerListView != null) {
      builtinPickerListView.getItems().setAll(
          BuiltInVehicleCatalog.idsNotYetInCatalog(components));
    }
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
    pane.getStyleClass().add("screen-root");
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
    mainCharacterIdField.setPromptText("(set via button below)");
    mainCharacterIdField.setPrefWidth(200);
    mainCharacterIdField.setEditable(false);
    mainCharacterIdField.setStyle("-fx-opacity: 1.0;");

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
        buildComponentsTab()
    );
    VBox.setVgrow(tabs, Priority.ALWAYS);
    return tabs;
  }

  private Tab buildTerrainTab() {
    ListView<String> list = new ListView<>();
    terrainListView = list;
    refreshTerrainList(list);

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
      refreshTerrainList(list);
      clearFields(idF, spriteF, colorF, frictionF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = list.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        terrainTypes.remove(idx);
        refreshTerrainList(list);
      }
    });

    return new Tab("Terrain", buildTabContent(list,
        formRow("ID", idF), spriteRow(spriteF),
        formRow("Color", colorF), formRow("Friction", frictionF),
        buttonBar(add, remove)));
  }

  private void refreshTerrainList(ListView<String> list) {
    if (terrainTypes == null) {
      return;
    }
    list.getItems().setAll(terrainTypes.stream()
        .map(t -> t.id() + "  [friction=" + t.friction() + "]").toList());
  }

  private Tab buildStaticTab() {
    ListView<String> list = new ListView<>();
    staticListView = list;
    refreshStaticList(list);

    TextField idF = field("id");
    TextField spriteF = field("spriteUrl");
    TextField massF = field("mass, e.g. 0");
    TextField hpChangeF = field("+25 heals, -25 damages");

    Label hpLabel = new Label("HP Change:");
    hpLabel.setPrefWidth(120);
    HBox hpRow = new HBox(8, hpLabel, hpChangeF);

    Label massLabel = new Label("Mass:");
    massLabel.setPrefWidth(120);
    HBox massRow = new HBox(8, massLabel, massF);

    Button addHpObject = new Button("Add HP Object");
    addHpObject.setOnAction(e -> {
      double hpChange = parseDouble(hpChangeF.getText(), 0);
      if (hpChange == 0) {
        new AuthoringAlertService().showError("Invalid HP",
            "HP change must be nonzero. Use positive to heal, negative to damage.");
        return;
      }
      CollisionAction action = buildHpAction(hpChange);
      staticComponents.add(new StaticComponentInfo(
          idF.getText().strip(),
          spriteF.getText().strip(),
          parseDouble(massF.getText(), 0),
          action));
      refreshStaticList(list);
      clearFields(idF, spriteF, massF, hpChangeF);
    });

    Button addFinishLine = new Button("Add Finish Line");
    addFinishLine.setOnAction(e -> {
      String finishId = idF.getText().strip().isBlank() ? "GoalLine" : idF.getText().strip();
      staticComponents.add(new StaticComponentInfo(
          finishId,
          spriteF.getText().strip(),
          0,
          new CollisionAction("finish", 0)));
      refreshStaticList(list);
      clearFields(idF, spriteF, massF, hpChangeF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = list.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        staticComponents.remove(idx);
        refreshStaticList(list);
      }
    });

    return new Tab("Static Objects", buildTabContent(list,
        formRow("ID", idF), spriteRow(spriteF),
        massRow, hpRow,
        buttonBar(addHpObject, addFinishLine, remove)));
  }

  private CollisionAction buildHpAction(double hpChange) {
    if (hpChange == 0) {
      return null;
    }
    if (hpChange > 0) {
      return new CollisionAction("heal", hpChange);
    }
    return new CollisionAction("damage", Math.abs(hpChange));
  }

  private void refreshStaticList(ListView<String> list) {
    if (staticComponents == null) {
      return;
    }
    list.getItems().setAll(staticComponents.stream()
        .map(s -> s.id() + formatHpChange(s.onCollisionAction())).toList());
  }

  private Tab buildComponentsTab() {
    ListView<String> list = new ListView<>();
    componentListView = list;
    refreshComponentList(list);

    // TODO: Refactor
    ListView<String> presetPicker = new ListView<>();
    presetPicker.setPrefHeight(100);
    presetPicker.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    builtinPickerListView = presetPicker;

    Label presetLabel = new Label("Preset parts (add to catalog):");
    Button addSelectedPresets = new Button("Add selected presets");
    addSelectedPresets.setOnAction(e -> {
      for (String id : new ArrayList<>(presetPicker.getSelectionModel().getSelectedItems())) {
        BuiltInVehicleCatalog.templateById(id)
            .ifPresent(template -> {
              if (components.stream().noneMatch(c -> c.id().equals(template.id()))) {
                components.add(template);
              }
            });
      }
      refreshComponentList(list);
      refreshBuiltinPicker();
    });

    VBox presetBlock = new VBox(6, presetLabel, presetPicker,
        new HBox(8, addSelectedPresets));
    refreshBuiltinPicker();

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
      refreshComponentList(list);
      refreshBuiltinPicker();
      clearFields(idF, spriteF, massF);
    });

    Button useAsMainCharacter = new Button("Use as Main Character");
    useAsMainCharacter.setOnAction(e -> selectedComponentId(list)
        .ifPresent(id -> {
          mainCharacterIdField.setText(id);
          new AuthoringAlertService().showInfo(
              "Main Character Set", "Main character set to: " + id);
        }));

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = list.getSelectionModel().getSelectedIndex();
      if (idx >= 0) {
        String removedId = components.get(idx).id();
        components.remove(idx);
        if (removedId.equals(mainCharacterIdField.getText().strip())) {
          mainCharacterIdField.clear();
        }
        refreshComponentList(list);
        refreshBuiltinPicker();
      }
    });

    VBox form = new VBox(12,
        presetBlock,
        new Label("Custom component"),
        formRow("ID", idF), spriteRow(spriteF), formRow("Mass", massF),
        new Label("Buttons:"), btnList,
        formRow("Button Name", btnNameF), formRow("Dir X", btnDxF), formRow("Dir Y", btnDyF),
        buttonBar(addBtn),
        buttonBar(add, useAsMainCharacter, remove));

    return new Tab("Components", buildTabContent(list, form));
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
      new AuthoringAlertService().showError("Save Error", e.getMessage());
    }
  }

  private Path saveCatalogForGameId(String gameId) throws IOException {
    if (gameId.isBlank()) {
      throw new IllegalArgumentException("Game ID is required before saving a game catalog.");
    }
    Path catalogPath = catalogPathFor(gameId);
    Files.createDirectories(catalogPath.getParent());
    Files.createDirectories(LEVELS_DIR.resolve(gameId));
    String mainCharacterId = mainCharacterIdField.getText().strip();
    validateMainCharacterId(mainCharacterId);
    catalogSaver.save(catalogPath.toString(),
        gameId,
        mainCharacterId,
        components, staticComponents, terrainTypes);
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
    return GAMES_DIR.resolve(gameId).resolve(GAME_FILE_NAME);
  }

  private GameConfig saveAndLoadCurrentConfig() {
    try {
      String gameId = gameIdField.getText().strip();
      Path catalogPath = saveCatalogForGameId(gameId);
      saveFilePath = catalogPath.toString();
      return new GameConfigImpl(saveFilePath);
    } catch (IOException | RuntimeException e) {
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

  private Node buildTabContent(ListView<String> list, Node... formNodes) {
    list.setPrefWidth(260);
    VBox form = new VBox(6, formNodes);
    form.setPadding(new Insets(8));
    HBox content = new HBox(12, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(8));
    return content;
  }

  private Node buildTabContent(ListView<String> list, VBox form) {
    list.setPrefWidth(260);
    form.setPadding(new Insets(8));
    HBox content = new HBox(12, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(8));
    return content;
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
    bar.setAlignment(Pos.CENTER_LEFT);
    return bar;
  }

  private TextField field(String prompt) {
    TextField f = new TextField();
    f.setPromptText(prompt);
    f.setPrefWidth(220);
    return f;
  }

  private void clearFields(TextField... fields) {
    for (TextField f : fields) {
      f.clear();
    }
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
    return "  [" + action.type() + "]";
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
