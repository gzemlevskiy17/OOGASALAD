package oogasalad.view.authoring.internal.level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.authoring.internal.common.AuthoringAlertService;
import oogasalad.view.core.internal.SpriteCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Scrollable vehicle-component palette displayed as one long horizontal row of component cells.
 * @author Emilio Garcia
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private static final Logger LOG = LogManager.getLogger(VehicleComponentScrollPalette.class);

  private final ScrollPane visualRoot;
  private final HBox paletteContent;
  private final VehiclePaletteConfig config;
  private final VehicleComponentSelectionModel selectionModel;
  private final VehicleComponentCellFactory cellFactory;
  private final AuthoringAlertService alertService;
  private final Map<String, StackPane> cellsById = new LinkedHashMap<>();
  private Consumer<String> selectionSummaryHandler = summary -> {
  };
  private Consumer<List<String>> selectedComponentsHandler = selectedComponents -> {
  };

  /**
   * Creates the vehicle component palette view.
   */
  public VehicleComponentScrollPalette() {
    this(VehiclePaletteConfig.loadDefault(), new AuthoringAlertService(), new SpriteCache());
  }

  VehicleComponentScrollPalette(VehiclePaletteConfig config, AuthoringAlertService alertService,
      SpriteCache spriteCache) {
    this.config = config;
    this.alertService = alertService;
    this.selectionModel = new VehicleComponentSelectionModel(config.defaultComponentNames(),
        config.maxSelectableItems());
    this.cellFactory = new VehicleComponentCellFactory(config, spriteCache);
    paletteContent = new HBox(config.itemSpacing());
    paletteContent.setPadding(new Insets(config.contentPadding()));
    paletteContent.setAlignment(Pos.CENTER);
    paletteContent.getStyleClass().add("authoring-level-palette");
    rebuildCells();

    visualRoot = new ScrollPane(paletteContent);
    visualRoot.setFitToWidth(false);
    visualRoot.setFitToHeight(true);
    visualRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    visualRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    visualRoot.setPannable(true);
    visualRoot.setPrefViewportHeight(config.viewportHeight());
    visualRoot.setPrefHeight(config.viewportHeight());
    visualRoot.setMinHeight(config.viewportHeight());
    visualRoot.setMaxHeight(config.viewportHeight());
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
    return selectionModel.maxSelectableItems();
  }

  public void setAvailableComponentIds(List<String> componentIds) {
    selectionModel.setAvailableComponentIds(componentIds);
    rebuildCells();
    notifySelectionSummaryChanged();
  }

  public void setSelectedComponentIds(List<String> componentIds) {
    selectionModel.setSelectedComponentIds(componentIds);
    refreshCellStyles();
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

  private void toggleSelection(SelectedVehicleComponent component) {
    VehicleComponentSelectionModel.ToggleResult result = selectionModel.toggle(component);
    if (result == VehicleComponentSelectionModel.ToggleResult.REJECTED_LIMIT) {
      LOG.debug("Rejected vehicle component selection '{}' because the palette limit was reached",
          component.id());
      alertService.showWarning(config.selectionLimitTitle(), config.selectionLimitMessage());
      return;
    }
    refreshCellStyles();
    notifySelectionSummaryChanged();
  }

  private void notifySelectionSummaryChanged() {
    selectedComponentsHandler.accept(selectionModel.selectedIds());
    if (selectionModel.selectedComponents().isEmpty()) {
      selectionSummaryHandler.accept(config.selectionSummaryEmpty());
      return;
    }
    selectionSummaryHandler.accept(selectionModel.selectedComponents().stream()
        .map(SelectedVehicleComponent::displayName)
        .collect(java.util.stream.Collectors.joining(", ")));
  }

  private void rebuildCells() {
    paletteContent.getChildren().clear();
    cellsById.clear();
    for (SelectedVehicleComponent component : selectionModel.availableComponents()) {
      StackPane cell = cellFactory.createCell(component, this::toggleSelection);
      cellsById.put(component.id(), cell);
      paletteContent.getChildren().add(cell);
    }
  }

  private void refreshCellStyles() {
    for (Map.Entry<String, StackPane> entry : cellsById.entrySet()) {
      cellFactory.setSelected(entry.getValue(), selectionModel.isSelected(entry.getKey()));
    }
  }
}
