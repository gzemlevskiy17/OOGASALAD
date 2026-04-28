package oogasalad.view.authoring.internal;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.core.internal.SpriteCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Scrollable vehicle-component palette displayed as one long horizontal row of component cells.
 * @author Emilio Garcia
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private static final Logger LOG = LogManager.getLogger(VehicleComponentScrollPalette.class);
  private static final ResourceBundle PALETTE_CONFIG =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");
  private static final String UNSELECTED_CELL_STYLE = """
      -fx-border-color: #707070;
      -fx-border-width: 1;
      -fx-background-color: white;
      """;

  private final ScrollPane visualRoot;
  private final HBox paletteContent;
  private final List<String> componentNames = new ArrayList<>();
  private final SpriteCache spriteCache = new SpriteCache();
  private final AuthoringAlertService alertService = new AuthoringAlertService();
  private final Map<StackPane, SelectedVehicleComponent> selectedCells = new LinkedHashMap<>();
  private Consumer<String> selectionSummaryHandler = summary -> {
  };
  private Consumer<List<String>> selectedComponentsHandler = selectedComponents -> {
  };

  /**
   * Creates the vehicle component palette view.
   */
  public VehicleComponentScrollPalette() {
    double contentPadding = Double.parseDouble(PALETTE_CONFIG.getString("contentPadding"));

    paletteContent = new HBox(Double.parseDouble(PALETTE_CONFIG.getString("itemSpacing")));
    paletteContent.setPadding(new Insets(contentPadding));
    paletteContent.setAlignment(Pos.CENTER);
    paletteContent.getStyleClass().add("authoring-level-palette");
    componentNames.addAll(defaultComponentNames());
    rebuildCells();

    double paletteHeight = Double.parseDouble(PALETTE_CONFIG.getString("viewportHeight"));
    visualRoot = new ScrollPane(paletteContent);
    visualRoot.setFitToWidth(false);
    visualRoot.setFitToHeight(true);
    visualRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    visualRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    visualRoot.setPannable(true);
    visualRoot.setPrefViewportHeight(paletteHeight);
    visualRoot.setPrefHeight(paletteHeight);
    visualRoot.setMinHeight(paletteHeight);
    visualRoot.setMaxHeight(paletteHeight);
    visualRoot.getStyleClass().add("authoring-level-palette-scroll");
    notifySelectionSummaryChanged();
  }

  @Override
  public Node getVisualRoot() {
    return visualRoot;
  }

  /**
   * Returns the maximum number of vehicle items the palette allows to be selected.
   *
   * @return configured vehicle-item selection cap
   */
  public int getMaxSelectableItems() {
    return Integer.parseInt(PALETTE_CONFIG.getString("maxSelectableItems"));
  }

  public void setAvailableComponentIds(List<String> componentIds) {
    componentNames.clear();
    if (componentIds == null || componentIds.isEmpty()) {
      componentNames.addAll(defaultComponentNames());
    } else {
      for (String componentName : componentIds) {
        String name = componentName == null ? "" : componentName.trim();
        if (!name.isBlank() && !componentNames.contains(name)) {
          componentNames.add(name);
        }
      }
    }
    selectedCells.clear();
    rebuildCells();
    notifySelectionSummaryChanged();
  }

  /**
   * Registers a listener that is notified whenever the selected vehicle item summary changes.
   *
   * @param handler consumer that receives the formatted selected-items summary text
   */
  public void setSelectionSummaryHandler(Consumer<String> handler) {
    selectionSummaryHandler = handler == null ? summary -> {
    } : handler;
    notifySelectionSummaryChanged();
  }

  /**
   * Registers a listener that is notified whenever the selected vehicle component IDs change.
   *
   * @param handler consumer that receives the current selected vehicle component IDs
   */
  public void setSelectedComponentsHandler(Consumer<List<String>> handler) {
    selectedComponentsHandler = handler == null ? selectedComponents -> {
    } : handler;
    notifySelectionSummaryChanged();
  }

  /**
   * Builds one selectable component cell containing the sprite preview and component name.
   *
   * @param componentName display name for the component
   * @return configured selectable palette cell
   */
  private StackPane createComponentCell(String componentName) {
    Label label = new Label(componentName);
    label.setWrapText(true);
    label.setMaxWidth(Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth")));
    label.setAlignment(Pos.CENTER);
    label.setTextAlignment(TextAlignment.CENTER);
    label.setStyle("-fx-font-size: 10px;");

    VBox cellContent = new VBox(2);
    cellContent.setAlignment(Pos.CENTER);

    String resourceKey = componentName.replace(" ", "");
    String componentId = componentName;
    String spriteKey = resourceKey + ".sprite";
    if (PALETTE_CONFIG.containsKey(spriteKey)) {
      Image img = spriteCache.get(PALETTE_CONFIG.getString(spriteKey));
      if (img != null) {
        double iconSize = Double.parseDouble(PALETTE_CONFIG.getString("cellIconSize"));
        ImageView icon = new ImageView(img);
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);
        cellContent.getChildren().add(icon);
      }
    }

    cellContent.getChildren().add(label);

    StackPane cell = new StackPane(cellContent);
    cell.setAlignment(Pos.CENTER);
    double cellWidth = Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth"))
        + 2 * Double.parseDouble(PALETTE_CONFIG.getString("cellInset"));
    cell.setMinWidth(cellWidth);
    cell.setPrefWidth(cellWidth);
    cell.setPrefHeight(Double.parseDouble(PALETTE_CONFIG.getString("cellPrefHeight")));
    cell.setPadding(new Insets(Double.parseDouble(PALETTE_CONFIG.getString("cellInset"))));
    updateCellStyle(cell, false);
    cell.setOnMouseClicked(event -> {
      try {
        toggleSelection(cell, new SelectedVehicleComponent(componentId, componentName));
      } catch (VehicleSelectionLimitException e) {
        LOG.warn("{}", e.getMessage());
        alertService.showWarning(
            PALETTE_CONFIG.getString("selectionLimitTitle"),
            e.getMessage());
      }
    });
    HBox.setHgrow(cell, Priority.NEVER);
    return cell;
  }

  private void toggleSelection(StackPane cell, SelectedVehicleComponent component) {
    if (selectedCells.containsKey(cell)) {
      selectedCells.remove(cell);
      updateCellStyle(cell, false);
      notifySelectionSummaryChanged();
      return;
    }
    validateSelectionCapacity();
    selectedCells.put(cell, component);
    updateCellStyle(cell, true);
    notifySelectionSummaryChanged();
  }

  private void validateSelectionCapacity() {
    int maxSelectable = Integer.parseInt(PALETTE_CONFIG.getString("maxSelectableItems"));
    if (selectedCells.size() >= maxSelectable) {
      throw new VehicleSelectionLimitException(
          PALETTE_CONFIG.getString("selectionLimitMessage"));
    }
  }

  private void notifySelectionSummaryChanged() {
    selectedComponentsHandler.accept(selectedCells.values().stream()
        .map(SelectedVehicleComponent::id)
        .toList());
    if (selectedCells.isEmpty()) {
      selectionSummaryHandler.accept(PALETTE_CONFIG.getString("selectionSummaryEmpty"));
      return;
    }
    selectionSummaryHandler.accept(selectedCells.values().stream()
        .map(SelectedVehicleComponent::displayName)
        .collect(java.util.stream.Collectors.joining(", ")));
  }

  private void rebuildCells() {
    paletteContent.getChildren().clear();
    for (String componentName : componentNames) {
      paletteContent.getChildren().add(createComponentCell(componentName));
    }
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

  private record SelectedVehicleComponent(String id, String displayName) {
  }

  private void updateCellStyle(StackPane cell, boolean selected) {
    if (!selected) {
      cell.setStyle(UNSELECTED_CELL_STYLE);
      return;
    }
    cell.setStyle("""
        -fx-border-color: #e67e22;
        -fx-border-width: 3;
        -fx-background-color: white;
        """);
  }
}
