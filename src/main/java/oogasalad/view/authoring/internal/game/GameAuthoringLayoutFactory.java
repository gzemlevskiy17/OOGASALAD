package oogasalad.view.authoring.internal.game;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.view.authoring.internal.common.AuthoringAlertService;
import oogasalad.view.authoring.internal.common.AuthoringValueHints;

/**
 * Builds the game authoring screen shell and wires its UI controls to screen callbacks.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class GameAuthoringLayoutFactory {

  private static final double DEFAULT_WIDTH = 220;
  private static final double ACTION_BUTTON_WIDTH = 120;
  private static final double ACTION_BUTTON_HEIGHT = 30;
  private static final double HEADER_SPACING = 8;
  private static final double META_ROW_SPACING = 12;
  private static final double TOP_ROW_SPACING = 16;
  private static final double FOOTER_SPACING = 12;

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.GameAuthoring");

  private final GameCatalogDraft draft;
  private final SpriteImportService spriteImportService;
  private final AuthoringAlertService alertService;
  private final CheatAuthoringValidator cheatValidator;
  private final Supplier<String> mainCharacterIdSupplier;
  private final Consumer<String> mainCharacterIdSetter;
  private final Consumer<Button> loadCatalogHandler;
  private final Consumer<Button> loadLevelHandler;
  private final Runnable saveHandler;
  private final Runnable createLevelHandler;
  private final Runnable backHandler;

  GameAuthoringLayoutFactory(GameCatalogDraft draft, SpriteImportService spriteImportService,
      AuthoringAlertService alertService, CheatAuthoringValidator cheatValidator,
      Supplier<String> mainCharacterIdSupplier, Consumer<String> mainCharacterIdSetter,
      Consumer<Button> loadCatalogHandler, Consumer<Button> loadLevelHandler,
      Runnable saveHandler, Runnable createLevelHandler, Runnable backHandler) {
    this.draft = Objects.requireNonNull(draft);
    this.spriteImportService = Objects.requireNonNull(spriteImportService);
    this.alertService = Objects.requireNonNull(alertService);
    this.cheatValidator = Objects.requireNonNull(cheatValidator);
    this.mainCharacterIdSupplier = Objects.requireNonNull(mainCharacterIdSupplier);
    this.mainCharacterIdSetter = Objects.requireNonNull(mainCharacterIdSetter);
    this.loadCatalogHandler = Objects.requireNonNull(loadCatalogHandler);
    this.loadLevelHandler = Objects.requireNonNull(loadLevelHandler);
    this.saveHandler = Objects.requireNonNull(saveHandler);
    this.createLevelHandler = Objects.requireNonNull(createLevelHandler);
    this.backHandler = Objects.requireNonNull(backHandler);
  }

  Parts build() {
    TextField gameIdField = gameIdField();
    TextField mainCharacterIdField = mainCharacterIdField();
    TerrainCatalogTab terrainTab = new TerrainCatalogTab(draft, spriteImportService);
    StaticObjectCatalogTab staticObjectTab = new StaticObjectCatalogTab(draft,
        spriteImportService, alertService);
    ComponentCatalogTab componentTab = new ComponentCatalogTab(draft, spriteImportService,
        alertService, mainCharacterIdSupplier, mainCharacterIdSetter);
    CheatCatalogTab cheatTab = new CheatCatalogTab(draft, cheatValidator, alertService,
        mainCharacterIdSupplier);

    BorderPane layout = buildRootPane();
    layout.setTop(buildHeader(gameIdField, mainCharacterIdField));
    layout.setCenter(buildCatalogEditor(terrainTab, staticObjectTab, componentTab, cheatTab));
    layout.setBottom(buildFooter());
    return new Parts(layout, gameIdField, mainCharacterIdField, terrainTab, staticObjectTab,
        componentTab, cheatTab);
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

  private VBox buildHeader(TextField gameIdField, TextField mainCharacterIdField) {
    Label title = new Label(s("titleText"));
    title.setFont(Font.font(d("titleFontSize")));
    title.getStyleClass().add("authoring-entry-title");

    HBox metaRow = new HBox(META_ROW_SPACING,
        new Label("Game ID:"), gameIdField,
        new Label("Main Character:"), mainCharacterIdField);
    metaRow.setAlignment(Pos.CENTER_LEFT);

    Button loadCatalogButton = new Button(s("loadCatalogButtonText"));
    loadCatalogButton.getStyleClass().add("authoring-entry-action");
    loadCatalogButton.setOnAction(e -> loadCatalogHandler.accept(loadCatalogButton));

    HBox topRow = new HBox(TOP_ROW_SPACING, metaRow, loadCatalogButton);
    topRow.setAlignment(Pos.CENTER_LEFT);

    VBox header = new VBox(HEADER_SPACING, title, topRow);
    header.getStyleClass().add("authoring-entry-center");
    return header;
  }

  private TabPane buildCatalogEditor(TerrainCatalogTab terrainTab,
      StaticObjectCatalogTab staticObjectTab, ComponentCatalogTab componentTab,
      CheatCatalogTab cheatTab) {
    TabPane tabs = new TabPane();
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getTabs().addAll(
        terrainTab.build(),
        staticObjectTab.build(),
        componentTab.build(),
        cheatTab.build()
    );
    VBox.setVgrow(tabs, Priority.ALWAYS);
    return tabs;
  }

  private HBox buildFooter() {
    Button saveButton = new Button(s("saveGameButtonText"));
    saveButton.getStyleClass().add("authoring-entry-action");
    saveButton.setOnAction(e -> saveHandler.run());

    Button createLevelButton = new Button(s("createNewLevelButtonText"));
    configureActionButton(createLevelButton);
    createLevelButton.setOnAction(e -> createLevelHandler.run());

    Button loadLevelButton = new Button(s("loadExistingLevelButtonText"));
    configureActionButton(loadLevelButton);
    loadLevelButton.setOnAction(e -> loadLevelHandler.accept(loadLevelButton));

    Button backButton = new Button(s("backButtonText"));
    backButton.getStyleClass().add("authoring-entry-back");
    backButton.setOnAction(e -> backHandler.run());

    HBox footer = new HBox(FOOTER_SPACING, backButton, saveButton,
        createLevelButton, loadLevelButton);
    footer.setAlignment(Pos.CENTER_LEFT);
    footer.setPadding(new Insets(d("buttonRegionPadding"), 0, 0, 0));
    footer.getStyleClass().add("authoring-entry-button-row");
    return footer;
  }

  private TextField gameIdField() {
    TextField field = new TextField();
    field.setPrefWidth(DEFAULT_WIDTH);
    AuthoringValueHints.configure(field, "catalog.meta.gameId");
    return field;
  }

  private TextField mainCharacterIdField() {
    TextField field = new TextField();
    field.setPrefWidth(DEFAULT_WIDTH);
    AuthoringValueHints.configure(field, "catalog.meta.mainCharacter");
    field.setEditable(false);
    field.setStyle("-fx-opacity: 1.0;");
    return field;
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

  record Parts(Parent root, TextField gameIdField, TextField mainCharacterIdField,
               TerrainCatalogTab terrainTab, StaticObjectCatalogTab staticObjectTab,
               ComponentCatalogTab componentTab, CheatCatalogTab cheatTab) {
  }
}
