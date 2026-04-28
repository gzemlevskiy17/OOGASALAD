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
import javafx.scene.control.ColorPicker;
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
import javafx.scene.paint.Color;
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
  private static final double DEFAULT_WIDTH = 220;
  private static final double FORM_ROW_WIDTH = 120;
  private static final double ACTION_BUTTON_WIDTH = 120;
  private static final double ACTION_BUTTON_HEIGHT = 30;
  private static final double BUTTON_BAR_SPACING = 8;
  private static final double HEADER_SPACING = 8;
  private static final double FORM_ROW_SPACING = 6;
  private static final double FORM_ROW_PADDING = 8;
  private static final double CONTENT_PADDING = 8;
  private static final double SPRITE_ROW_SPACING = 8;
  private static final double BUTTON_LIST_HEIGHT = 80;
  private static final double META_ROW_SPACING = 12;
  private static final double TOP_ROW_SPACING = 16;
  private static final double DEFAULT_FRICTION = 0.5;
  private static final double FORM_SPACING = 6;
  private static final double FOOTER_SPACING = 12;
  private static final double DEFAULT_MASS = 1.0;
  private static final double DEFAULT_POWER_UP_MASS = 0;
  private static final double LIST_WIDTH = 260;
  private static final double TAB_CONTENT_SPACING = 12;
  private static final double DEFAULT_HP_CHANGE = 0;

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
    gameIdField.setPrefWidth(DEFAULT_WIDTH);
    AuthoringValueHints.configure(gameIdField, "catalog.meta.gameId");

    mainCharacterIdField = new TextField();
    mainCharacterIdField.setPrefWidth(DEFAULT_WIDTH);
    AuthoringValueHints.configure(mainCharacterIdField, "catalog.meta.mainCharacter");
    mainCharacterIdField.setEditable(false);
    mainCharacterIdField.setStyle("-fx-opacity: 1.0;");

    HBox metaRow = new HBox(META_ROW_SPACING,
        new Label("Game ID:"), gameIdField,
        new Label("Main Character:"), mainCharacterIdField);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    Button loadCatalogButton = new Button(s("loadCatalogButtonText"));
    loadCatalogButton.getStyleClass().add("authoring-entry-action");
    loadCatalogButton.setOnAction(e -> handleLoadCatalog(loadCatalogButton));

    HBox topRow = new HBox(TOP_ROW_SPACING, metaRow, loadCatalogButton);
    topRow.setAlignment(Pos.CENTER_LEFT);

    VBox header = new VBox(HEADER_SPACING, title, topRow);
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

    TextField idF = AuthoringValueHints.newTextField("catalog.terrain.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.terrain.spriteUrl");
    ColorPicker colorF = new ColorPicker(Color.WHITE);
    colorF.setPrefWidth(220);
    spriteF.textProperty().addListener((obs, oldVal, newVal) -> {
      colorF.setDisable(!newVal.isBlank());
    });
    TextField frictionF = AuthoringValueHints.newTextField("catalog.terrain.friction");

    Button add = new Button("Add");
    add.setOnAction(e -> {
      boolean hasSprite = !spriteF.getText().isBlank();
      String hex = null;
      if (!hasSprite) {
        Color c = colorF.getValue();
        hex = String.format("#%02x%02x%02x",
            (int) (c.getRed() * 255),
            (int) (c.getGreen() * 255),
            (int) (c.getBlue() * 255));
      }
      terrainTypes.add(new TerrainInfo(
          idF.getText().strip(),
          nullIfBlank(spriteF.getText()),
          hex,
          parseDouble(frictionF.getText(), DEFAULT_FRICTION)));
      refreshTerrainList(list);
      colorF.setValue(Color.WHITE);
      clearFields(idF, spriteF, frictionF);
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
        formRow("Color", colorF, 120), formRow("Friction", frictionF),
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

    TextField idF = AuthoringValueHints.newTextField("catalog.static.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.static.spriteUrl");
    TextField massF = AuthoringValueHints.newTextField("catalog.static.mass");
    TextField hpChangeF = AuthoringValueHints.newTextField("catalog.static.hpDelta");

    Label hpLabel = new Label("HP Change:");
    hpLabel.setPrefWidth(DEFAULT_WIDTH);
    HBox hpRow = new HBox(SPRITE_ROW_SPACING, hpLabel, hpChangeF);

    Label massLabel = new Label("Mass:");
    massLabel.setPrefWidth(DEFAULT_WIDTH);
    HBox massRow = new HBox(SPRITE_ROW_SPACING, massLabel, massF);

    Button addHpObject = new Button("Add HP Object");
    addHpObject.setOnAction(e -> {
      double hpChange = parseDouble(hpChangeF.getText(), DEFAULT_HP_CHANGE);
      if (hpChange == 0) {
        new AuthoringAlertService().showError("Invalid HP",
            "HP change must be nonzero. Use positive to heal, negative to damage.");
        return;
      }
      CollisionAction action = buildHpAction(hpChange);
      staticComponents.add(new StaticComponentInfo(
          idF.getText().strip(),
          spriteF.getText().strip(),
          parseDouble(massF.getText(), DEFAULT_MASS),
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
          DEFAULT_POWER_UP_MASS,
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
    if (hpChange == DEFAULT_HP_CHANGE) {
      return null;
    }
    if (hpChange > DEFAULT_HP_CHANGE) {
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

    TextField idF = AuthoringValueHints.newTextField("catalog.component.id");
    TextField spriteF = AuthoringValueHints.newTextField("catalog.component.spriteUrl");
    TextField massF = AuthoringValueHints.newTextField("catalog.component.mass");
    TextField btnNameF = AuthoringValueHints.newTextField("catalog.component.buttonLabel");
    TextField btnDxF = AuthoringValueHints.newTextField("catalog.component.directionX");
    TextField btnDyF = AuthoringValueHints.newTextField("catalog.component.directionY");

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

    List<ButtonInfo> pendingButtons = new ArrayList<>();
    ListView<String> btnList = new ListView<>();
    btnList.setPrefHeight(BUTTON_LIST_HEIGHT);

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
          parseDouble(massF.getText(), DEFAULT_POWER_UP_MASS),
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

    VBox form = new VBox(FORM_SPACING,
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

    HBox footer = new HBox(FOOTER_SPACING, backButton, saveButton, createLevelButton, loadLevelButton);
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
    list.setPrefWidth(LIST_WIDTH);
    VBox form = new VBox(FORM_ROW_SPACING, formNodes);
    form.setPadding(new Insets(FORM_ROW_PADDING));
    HBox content = new HBox(TAB_CONTENT_SPACING, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(CONTENT_PADDING));
    return content;
  }

  private Node buildTabContent(ListView<String> list, VBox form) {
    list.setPrefWidth(LIST_WIDTH);
    form.setPadding(new Insets(FORM_ROW_PADDING));
    HBox content = new HBox(TAB_CONTENT_SPACING, list, form);
    HBox.setHgrow(form, Priority.ALWAYS);
    content.setPadding(new Insets(CONTENT_PADDING));
    return content;
  }

  private HBox spriteRow(TextField spriteField) {
    Button browse = new Button("Browse…");
    browse.setOnAction(e -> browseSprite(browse, spriteField));
    Label lbl = new Label("Sprite:");
    lbl.setPrefWidth(DEFAULT_WIDTH);
    HBox.setHgrow(spriteField, Priority.ALWAYS);
    return new HBox(SPRITE_ROW_SPACING, lbl, spriteField, browse);
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
    lbl.setPrefWidth(DEFAULT_WIDTH);
    return new HBox(FORM_ROW_WIDTH, lbl, field);
  }

  private HBox formRow(String label, Node control, double labelWidth) {
    Label lbl = new Label(label + ":");
    lbl.setPrefWidth(labelWidth);
    return new HBox(8, lbl, control);
  }

  private HBox buttonBar(Button... buttons) {
    HBox bar = new HBox(BUTTON_BAR_SPACING, buttons);
    bar.setAlignment(Pos.CENTER_LEFT);
    return bar;
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
    button.setPrefSize(ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
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
