package oogasalad.view.player.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.GameConfig;
import oogasalad.model.player.api.BuildRulesRegistry;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.ValidationResult;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.model.player.internal.PlacementValidator;
import oogasalad.view.core.internal.AbstractAppScreen;
import oogasalad.view.core.internal.PaletteSpriteRegistry;
import oogasalad.view.core.internal.SpriteCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Screen on which the player builds a vehicle.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 */
public class VehicleBuilderAppScreen extends AbstractAppScreen {

  private static final Logger LOG = LogManager.getLogger(VehicleBuilderAppScreen.class);
  private static final ResourceBundle PROPS = ResourceBundle.getBundle(
      "oogasalad.view.player.VehicleBuilderAppScreen");
  private static final ResourceBundle PALETTE_CONFIG = ResourceBundle.getBundle(
      "oogasalad.view.authoring.VehicleComponentPalette");
  private static final double CELL_SIZE = 60.0;
  private static final String REMOVE_PAYLOAD = "__REMOVE__";

  private final SpriteCache spriteCache;
  private final PaletteSpriteRegistry spriteRegistry;
  private final BuildRulesRegistry buildRules;
  private final Map<String, List<String>> gridState = new HashMap<>();
  private final List<StackPane> gridCells;
  private List<String> availableComponentNames;
  private FlowPane componentFlow;
  private PlacementValidator validator;
  private Label errorLabel;
  private String mainCharacterId = "";
  private boolean hasGameConfig;

  public VehicleBuilderAppScreen(Consumer<Outcome> onOutcome,
      SpriteCache spriteCache, PaletteSpriteRegistry spriteRegistry,
      BuildRulesRegistry buildRules) {
    this.spriteCache = spriteCache;
    this.spriteRegistry = spriteRegistry;
    this.buildRules = buildRules;
    this.gridCells = new ArrayList<>();
    // super() calls createRoot(), so the fields above must be assigned first.
    super(onOutcome);
  }


  private static String key(int row, int col) {
    return row + "," + col;
  }

  private static int[] parseKey(String key) {
    String[] parts = key.split(",");
    return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
  }

  private Node createTitleRegion() {
    Label title = new Label(s("title.text"));
    title.getStyleClass().add("header-title");
    return title;
  }

  private Node createStartLevelRegion() {
    Button startButton = new Button(s("start.button.text"));
    startButton.getStyleClass().add("start-button");
    startButton.setOnAction(event -> {
      if (!hasMainCharacterInBuild()) {
        showError("not able to start the game");
        return;
      }
      onOutcome.accept(Outcome.OPEN_GAMEPLAY);
    });

    VBox container = new VBox(startButton);
    container.getStyleClass().add("selection-region");
    container.setAlignment(Pos.CENTER);
    return container;
  }

  private VBox createComponentSection() {
    VBox container = new VBox(d("component.spacing"));
    container.setAlignment(Pos.CENTER);

    Text title = new Text(s("component.title.text"));
    title.getStyleClass().add("component-title");

    componentFlow = new FlowPane();
    componentFlow.setHgap(8);
    componentFlow.setVgap(8);
    componentFlow.setAlignment(Pos.CENTER);
    componentFlow.getStyleClass().add("component-grid");
    rebuildComponentPalette();

    ScrollPane scroll = new ScrollPane(componentFlow);
    scroll.setFitToWidth(true);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setMinHeight(d("component.palette.height"));
    VBox.setVgrow(scroll, Priority.ALWAYS);

    container.getChildren().addAll(title, scroll);
    return container;
  }

  /**
   * Provides the game catalog so the builder can read component names and sprites from
   * game.json instead of the hardcoded properties file.
   */
  public void setGameConfig(GameConfig config) {
    hasGameConfig = config != null;
    if (config != null) {
      mainCharacterId = config.mainCharacterId();
      buildRules.registerFromConfig(config);
      spriteRegistry.registerFromConfig(config);
    } else {
      mainCharacterId = "";
    }
    ensureAvailableComponentNames();
    if (componentFlow != null) {
      rebuildComponentPalette();
    }
  }

  /**
   * Restricts the vehicle builder palette to the ids allowed by the selected level.
   */
  public void setAllowedComponentIds(List<String> componentIds) {
    ensureAvailableComponentNames();
    availableComponentNames.clear();

    // TODO: maybe remove the check
    if (componentIds == null || componentIds.isEmpty()) {
      availableComponentNames.addAll(defaultComponentNames());
    } else {
      for (String componentName : componentIds) {
        String name = componentName == null ? "" : componentName.trim();
        if (!name.isBlank() && !availableComponentNames.contains(name)) {
          availableComponentNames.add(name);
        }
      }
    }

    ensureMainCharacterAvailable();
    gridState.clear();
    resetWorkspace();
    if (componentFlow != null) {
      rebuildComponentPalette();
    }
    clearError();
  }

  private static List<String> defaultComponentNames() {
    List<String> result = new ArrayList<>();
    for (String name : PALETTE_CONFIG.getString("componentNames").split(",")) {
      if (!name.isBlank()) {
        result.add(name.trim());
      }
    }
    return result;
  }

  private void ensureMainCharacterAvailable() {
    if (mainCharacterId != null && !mainCharacterId.isBlank()
        && !availableComponentNames.contains(mainCharacterId)) {
      availableComponentNames.add(mainCharacterId);
    }
  }

  private boolean hasMainCharacterInBuild() {
    if (!hasGameConfig) {
      return true;
    }
    if (mainCharacterId == null || mainCharacterId.isBlank()) {
      return false;
    }
    return getRawVehicleLayout().placements().stream()
        .anyMatch(component -> mainCharacterId.equals(component.componentType()));
  }

  private void rebuildComponentPalette() {
    ensureAvailableComponentNames();
    componentFlow.getChildren().clear();
    for (String name : availableComponentNames) {
      componentFlow.getChildren().add(createDraggableComponentCell(name));
    }
  }

  private void ensureAvailableComponentNames() {
    if (availableComponentNames == null) {
      availableComponentNames = new ArrayList<>(defaultComponentNames());
    }
  }

  /**
   * Reads the current grid state and returns it as a VehicleLayout. Called by the gameplay factory
   * just before the session starts. Each stacked component becomes its own {@link PlacedComponent}.
   */
  public VehicleLayout getVehicleLayout() {
    return getRawVehicleLayout();
  }

  private VehicleLayout getRawVehicleLayout() {
    List<PlacedComponent> placements = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : gridState.entrySet()) {
      int[] rc = parseKey(entry.getKey());
      for (String type : entry.getValue()) {
        placements.add(new PlacedComponent(rc[0], rc[1], type));
      }
    }
    return new VehicleLayout(placements);
  }

  @Override
  public void onEnter() {
    resetWorkspace();
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(getClass().getResource(
        "/oogasalad/view/player/vehicle-builder.css").toExternalForm());
    root.getStyleClass().add("screen-root");
    VBox mainContent = new VBox(d("main.content.spacing"));
    mainContent.setAlignment(Pos.CENTER);
    mainContent.getChildren().addAll(
        createTitleRegion(),
        createGridRegion(),
        createComponentSection(),
        createStartLevelRegion()
    );

    root.setCenter(mainContent);
    root.setBottom(createBackButtonRegion());
    return root;
  }

  private Node createDraggableComponentCell(String componentName) {
    VBox cell = new VBox(4);
    cell.setAlignment(Pos.CENTER);
    cell.getStyleClass().add("component-cell");
    cell.getChildren().add(buildSpritePreview(componentName));

    Label label = new Label(componentName);
    label.setWrapText(true);
    label.setMaxWidth(Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth")));
    cell.getChildren().add(label);

    cell.setOnDragDetected(e -> {
      Dragboard db = cell.startDragAndDrop(TransferMode.COPY);
      ClipboardContent content = new ClipboardContent();
      content.putString(componentName);
      db.setContent(content);
      e.consume();
    });
    return cell;
  }

  private Node buildSpritePreview(String componentName) {
    return spriteRegistry.resolveByType(componentName).renderPreview(spriteCache);
  }

  private Node createGridRegion() {
    GridPane grid = new GridPane();
    grid.getStyleClass().add("vehicle-grid");
    grid.setAlignment(Pos.CENTER);

    int cols = (int) d("grid.columns");
    int rows = (int) d("grid.rows");
    this.validator = new PlacementValidator(buildRules, rows, cols);

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grid-cell");
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(CELL_SIZE, CELL_SIZE);
        wireCellHandlers(cell, row, col);
        gridCells.add(cell);
        grid.add(cell, col, row);
      }
    }

    this.errorLabel = new Label();
    errorLabel.getStyleClass().add("build-error");
    errorLabel.setVisible(false);

    VBox region = new VBox(6, grid, errorLabel);
    region.setAlignment(Pos.CENTER);
    return region;
  }

  private void wireCellHandlers(StackPane cell, int row, int col) {
    cell.setOnDragOver(e -> {
      if (isPaletteDrag(e.getDragboard())) {
        e.acceptTransferModes(TransferMode.COPY);
      }
      e.consume();
    });

    cell.setOnDragEntered(e -> {
      if (isPaletteDrag(e.getDragboard())) {
        cell.getStyleClass().add("grid-cell-drag-hover");
      }
      e.consume();
    });

    cell.setOnDragExited(e -> {
      cell.getStyleClass().remove("grid-cell-drag-hover");
      e.consume();
    });

    cell.setOnDragDropped(e -> {
      Dragboard db = e.getDragboard();
      boolean placed = isPaletteDrag(db) && placeComponentInCell(cell, db.getString(), row, col);
      e.setDropCompleted(placed);
      e.consume();
    });

    // Drag a placed component off the grid to remove it.
    cell.setOnDragDetected(e -> {
      if (!gridState.containsKey(key(row, col))) {
        return;
      }
      Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      content.putString(REMOVE_PAYLOAD);
      db.setContent(content);
      e.consume();
    });

    cell.setOnDragDone(e -> {
      if (e.getTransferMode() == null) {
        popTopComponent(cell, row, col);
      }
      e.consume();
    });
  }

  private boolean isPaletteDrag(Dragboard db) {
    String s = db.getString();
    return s != null && !s.equals(REMOVE_PAYLOAD);
  }

  private boolean placeComponentInCell(StackPane cell, String componentName, int row, int col) {
    PlacedComponent candidate = new PlacedComponent(row, col, componentName);
    try {
      ValidationResult result = validator.canPlace(getVehicleLayout(), candidate, 0);
      if (!result.valid()) {
        showError(result.reason());
        return false;
      }
      clearError();

      List<String> stack = gridState.computeIfAbsent(key(row, col), k -> new ArrayList<>());
      if (buildRules.lookup(componentName).containment() == Containment.NONE) {
        stack.clear();
      }
      stack.add(componentName);
      renderStack(cell, stack);
      return true;
    } catch (IllegalArgumentException exception) {
      LOG.warn("Rejected vehicle component '{}' because placement rules are unavailable",
          componentName, exception);
      showError(exception.getMessage());
      return false;
    }
  }

  private void popTopComponent(StackPane cell, int row, int col) {
    String k = key(row, col);
    List<String> stack = gridState.get(k);
    if (stack == null || stack.isEmpty()) {
      return;
    }
    stack.remove(stack.size() - 1);
    if (stack.isEmpty()) {
      gridState.remove(k);
      cell.getChildren().clear();
    } else {
      renderStack(cell, stack);
    }
    clearError();
  }

  private void renderStack(StackPane cell, List<String> stack) {
    cell.getChildren().clear();
    for (String type : stack) {
      cell.getChildren().add(buildSpritePreview(type));
    }
  }

  private void resetWorkspace() {
    gridState.clear();
    for (StackPane cell : gridCells) {
      cell.getChildren().clear();
      cell.getStyleClass().remove("grid-cell-drag-hover");
    }
    clearError();
    LOG.debug("Reset vehicle builder workspace");
  }

  private void showError(String reason) {
    errorLabel.setText(reason);
    errorLabel.setVisible(true);
  }

  private void clearError() {
    errorLabel.setText("");
    errorLabel.setVisible(false);
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_PLAYER_ENTRY));

    StackPane container = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("nav-region");
    return container;
  }

  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
