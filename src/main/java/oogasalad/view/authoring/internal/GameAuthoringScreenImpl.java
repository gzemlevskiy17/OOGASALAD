package oogasalad.view.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.GameCatalogSaver;
import oogasalad.data.internal.GameConfigImpl;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Game catalog editor screen. Authors can define terrain types, static objects (pickups/hazards),
 * and movable components with their on-screen buttons. The catalog is saved to {@code game.json}
 * and then used to drive the level editor and vehicle builder.
 */
public class GameAuthoringScreenImpl extends AbstractAppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.GameAuthoring");

  private final Consumer<String> loadLevelHandler;
  private final Consumer<GameConfig> onGameConfigReady;
  private final FileChooserService fileChooserService;
  private final GameCatalogSaver catalogSaver = new GameCatalogSaver();

  // Mutable catalog state
  private final List<TerrainInfo> terrainTypes = new ArrayList<>();
  private final List<StaticComponentInfo> staticComponents = new ArrayList<>();
  private final List<ComponentInfo> components = new ArrayList<>();
  private TextField gameIdField;
  private TextField primaryComponentIdField;
  private String saveFilePath = "data/games/untitled/game.json";

  public GameAuthoringScreenImpl(Consumer<Outcome> onOutcome, Consumer<String> loadLevelHandler,
      Consumer<GameConfig> onGameConfigReady) {
    super(onOutcome);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.onGameConfigReady = onGameConfigReady != null ? onGameConfigReady : cfg -> {};
    this.fileChooserService = new FileChooserService();
  }

  /** Loads a game catalog from disk into the editor. */
  public void loadCatalogFromPath(String path) {
    try {
      GameConfig config = new GameConfigImpl(path);
      saveFilePath = path;
      gameIdField.setText(config.gameId());
      primaryComponentIdField.setText(config.primaryComponentId());
      terrainTypes.clear();
      terrainTypes.addAll(config.terrainTypes());
      staticComponents.clear();
      staticComponents.addAll(config.staticComponents());
      components.clear();
      components.addAll(config.components());
    } catch (Exception e) {
      new AuthoringAlertService().showError("Load Error", e.getMessage());
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

    primaryComponentIdField = new TextField();
    primaryComponentIdField.setPromptText("primary component id");
    primaryComponentIdField.setPrefWidth(200);

    HBox metaRow = new HBox(12,
        new Label("Game ID:"), gameIdField,
        new Label("Primary Component:"), primaryComponentIdField);
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

  // ── Terrain tab ──────────────────────────────────────────────────────────

  private Tab buildTerrainTab() {
    ListView<String> list = new ListView<>();
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
      if (idx >= 0) { terrainTypes.remove(idx); refreshTerrainList(list); }
    });

    return new Tab("Terrain", buildTabContent(list,
        formRow("ID", idF), formRow("Sprite URL", spriteF),
        formRow("Color", colorF), formRow("Friction", frictionF),
        buttonBar(add, remove)));
  }

  private void refreshTerrainList(ListView<String> list) {
    if (terrainTypes == null) return;
    list.getItems().setAll(terrainTypes.stream()
        .map(t -> t.id() + "  [friction=" + t.friction() + "]").toList());
  }

  // ── Static components tab ─────────────────────────────────────────────────

  private Tab buildStaticTab() {
    ListView<String> list = new ListView<>();
    refreshStaticList(list);

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
      refreshStaticList(list);
      clearFields(idF, spriteF, massF, actionTypeF, actionAmountF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = list.getSelectionModel().getSelectedIndex();
      if (idx >= 0) { staticComponents.remove(idx); refreshStaticList(list); }
    });

    return new Tab("Static Objects", buildTabContent(list,
        formRow("ID", idF), formRow("Sprite URL", spriteF),
        formRow("Mass", massF), formRow("Action Type", actionTypeF),
        formRow("Action Amount", actionAmountF),
        buttonBar(add, remove)));
  }

  private void refreshStaticList(ListView<String> list) {
    if (staticComponents == null) return;
    list.getItems().setAll(staticComponents.stream()
        .map(s -> s.id() + (s.onCollisionAction() != null
            ? "  [" + s.onCollisionAction().type() + "]" : "  [inert]")).toList());
  }

  // ── Movable components tab ────────────────────────────────────────────────

  private Tab buildComponentsTab() {
    ListView<String> list = new ListView<>();
    refreshComponentList(list);

    TextField idF = field("id");
    TextField spriteF = field("spriteUrl");
    TextField massF = field("mass, e.g. 1.0");
    TextField btnNameF = field("button name, e.g. go right");
    TextField btnDxF = field("direction x, e.g. 1");
    TextField btnDyF = field("direction y, e.g. 0");

    // Buttons list for the component being authored
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
      clearFields(idF, spriteF, massF);
    });

    Button remove = new Button("Remove");
    remove.setOnAction(e -> {
      int idx = list.getSelectionModel().getSelectedIndex();
      if (idx >= 0) { components.remove(idx); refreshComponentList(list); }
    });

    VBox form = new VBox(6,
        formRow("ID", idF), formRow("Sprite URL", spriteF), formRow("Mass", massF),
        new Label("Buttons:"), btnList,
        formRow("Button Name", btnNameF), formRow("Dir X", btnDxF), formRow("Dir Y", btnDyF),
        buttonBar(addBtn),
        buttonBar(add, remove));

    return new Tab("Components", buildTabContent(list, form));
  }

  private void refreshComponentList(ListView<String> list) {
    if (components == null) return;
    list.getItems().setAll(components.stream()
        .map(c -> c.id() + "  [" + c.buttons().size() + " buttons]").toList());
  }

  // ── Footer ────────────────────────────────────────────────────────────────

  private HBox buildFooter() {
    Button saveButton = new Button(s("saveGameButtonText"));
    saveButton.getStyleClass().add("authoring-entry-action");
    saveButton.setOnAction(e -> handleSaveCatalog(saveButton));

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

  private void handleSaveCatalog(Button source) {
    fileChooserService.chooseSaveFile(source.getScene().getWindow())
        .ifPresentOrElse(file -> {
          saveFilePath = file.getAbsolutePath();
          doSave();
        }, this::doSave);
  }

  private void doSave() {
    try {
      new java.io.File(saveFilePath).getParentFile().mkdirs();
      catalogSaver.save(saveFilePath,
          gameIdField.getText().strip(),
          primaryComponentIdField.getText().strip(),
          components, staticComponents, terrainTypes);
      new AuthoringAlertService().showInfo("Saved", "Game catalog saved to " + saveFilePath);
    } catch (Exception e) {
      new AuthoringAlertService().showError("Save Error", e.getMessage());
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
    try {
      // Save to a temp path so GameConfigImpl can parse it
      String tmpPath = "data/games/_current_game.json";
      new java.io.File(tmpPath).getParentFile().mkdirs();
      catalogSaver.save(tmpPath,
          gameIdField.getText().strip(),
          primaryComponentIdField.getText().strip(),
          components, staticComponents, terrainTypes);
      return new GameConfigImpl(tmpPath);
    } catch (Exception e) {
      return GameConfigImpl.empty();
    }
  }

  // ── UI helpers ────────────────────────────────────────────────────────────

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
    for (TextField f : fields) f.clear();
  }

  private String nullIfBlank(String s) {
    return (s == null || s.isBlank()) ? null : s.strip();
  }

  private double parseDouble(String text, double fallback) {
    try { return Double.parseDouble(text.strip()); }
    catch (NumberFormatException e) { return fallback; }
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(d("actionFontSize")));
    button.setPrefSize(d("actionButtonWidth"), d("actionButtonHeight"));
    button.setWrapText(true);
    button.getStyleClass().add("authoring-entry-action");
  }

  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
