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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.core.internal.AbstractAppScreen;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Screen on which the player builds a vehicle
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */

public class VehicleBuilderAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.VehicleBuilderAppScreen");
  private static final ResourceBundle PALETTE_CONFIG = ResourceBundle.getBundle(
      "oogasalad.view.authoring.VehicleComponentPalette");
  private static final SpriteCache spriteCache = new SpriteCache();

  private final Map<String, String> gridState = new HashMap<>();

  public VehicleBuilderAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
  }

  /**
   * Reads the current grid state and returns it as a VehicleLayout. Called by the gameplay factory
   * just before the session starts.
   */
  public VehicleLayout getVehicleLayout() {
    List<PlacedComponent> placements = new ArrayList<>();
    for (Map.Entry<String, String> entry : gridState.entrySet()) {
      String[] parts = entry.getKey().split(",");
      int row = Integer.parseInt(parts[0]);
      int col = Integer.parseInt(parts[1]);
      placements.add(new PlacedComponent(row, col, entry.getValue()));
    }
    return new VehicleLayout(placements);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/vehicle-builder.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    VBox mainContent = new VBox(d("main.content.spacing"));
    mainContent.setAlignment(Pos.CENTER);

    mainContent.getChildren().addAll(
        createTitleRegion(),
        createGridRegion(),
        createComponentSection(),
        createStartLevelRegion()
    );

    Node backButton = createBackButtonRegion();

    root.setCenter(mainContent);
    root.setBottom(backButton);

    return root;
  }

  private Node createTitleRegion() {
    Label title = new Label(s("title.text"));
    title.getStyleClass().add("header-title");
    return title;
  }

  private Node createStartLevelRegion() {
    Button startButton = new Button(s("start.button.text"));
    startButton.getStyleClass().add("start-button");
    startButton.setOnAction(event -> onOutcome.accept(Outcome.OPEN_GAMEPLAY));

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

    FlowPane componentFlow = new FlowPane();
    componentFlow.setHgap(8);
    componentFlow.setVgap(8);
    componentFlow.setAlignment(Pos.CENTER);
    componentFlow.getStyleClass().add("component-grid");

    for (String name : PALETTE_CONFIG.getString("componentNames").split(",")) {
      componentFlow.getChildren().add(createDraggableComponentCell(name.trim()));
    }

    ScrollPane scroll = new ScrollPane(componentFlow);
    scroll.setFitToWidth(true);
    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setMinHeight(d("component.palette.height"));
    VBox.setVgrow(scroll, Priority.ALWAYS);

    container.getChildren().addAll(title, scroll);
    return container;
  }

  private Node createDraggableComponentCell(String componentName) {
    VBox cell = new VBox(4);
    cell.setAlignment(Pos.CENTER);
    cell.getStyleClass().add("component-cell");

    String spriteKey = componentName.replace(" ", "") + ".sprite";
    if (PALETTE_CONFIG.containsKey(spriteKey)) {
      Image img = spriteCache.get(PALETTE_CONFIG.getString(spriteKey));
      if (img != null) {
        double iconSize = Double.parseDouble(PALETTE_CONFIG.getString("cellIconSize"));
        ImageView icon = new ImageView(img);
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);
        cell.getChildren().add(icon);
      }
    }

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

  private GridPane createGridRegion() {
    GridPane grid = new GridPane();
    grid.getStyleClass().add("vehicle-grid");
    grid.setAlignment(Pos.CENTER);

    int cols = (int) d("grid.columns");
    int rows = (int) d("grid.rows");

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grid-cell");
        wireDropTarget(cell, row, col);
        grid.add(cell, col, row);
      }
    }

    return grid;
  }

  private void wireDropTarget(StackPane cell, int row, int col) {
    cell.setOnDragOver(e -> {
      if (e.getDragboard().hasString()) {
        e.acceptTransferModes(TransferMode.COPY);
      }
      e.consume();
    });

    cell.setOnDragEntered(e -> {
      if (e.getDragboard().hasString()) {
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
      if (db.hasString()) {
        placeComponentInCell(cell, db.getString(), row, col);
        e.setDropCompleted(true);
      } else {
        e.setDropCompleted(false);
      }
      e.consume();
    });
  }

  private void placeComponentInCell(StackPane cell, String componentName, int row, int col) {
    gridState.put(row + "," + col, componentName);
    cell.getChildren().clear();

    String spriteKey = componentName.replace(" ", "") + ".sprite";
    if (PALETTE_CONFIG.containsKey(spriteKey)) {
      Image img = spriteCache.get(PALETTE_CONFIG.getString(spriteKey));
      if (img != null) {
        double iconSize = Double.parseDouble(PALETTE_CONFIG.getString("cellIconSize"));
        ImageView icon = new ImageView(img);
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);
        cell.getChildren().add(icon);
        return;
      }
    }
    cell.getChildren().add(new Label(componentName));
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
