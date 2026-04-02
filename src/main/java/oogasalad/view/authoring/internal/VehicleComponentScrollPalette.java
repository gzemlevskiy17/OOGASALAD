package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.constants.VehicleComponentPaletteConstants;

/**
 * Scrollable vehicle-component palette laid out in rows of eight equal-width cells.
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private final ScrollPane visualRoot;

  public VehicleComponentScrollPalette() {
    GridPane componentGrid = new GridPane();
    componentGrid.setMaxWidth(Double.MAX_VALUE);
    componentGrid.setPadding(new Insets(VehicleComponentPaletteConstants.CONTENT_PADDING));

    for (int i = 0; i < VehicleComponentPaletteConstants.COMPONENT_NAMES.length; i++) {
      ColumnConstraints constraints = new ColumnConstraints();
      constraints.setPercentWidth(100.0 / VehicleComponentPaletteConstants.COMPONENTS_PER_ROW);
      constraints.setHgrow(Priority.ALWAYS);
      componentGrid.getColumnConstraints().add(constraints);
    }

    RowConstraints rowConstraints = new RowConstraints();
    rowConstraints.setPrefHeight(VehicleComponentPaletteConstants.CELL_PREF_HEIGHT);
    rowConstraints.setVgrow(Priority.ALWAYS);
    componentGrid.getRowConstraints().add(rowConstraints);

    for (int i = 0; i < VehicleComponentPaletteConstants.COMPONENT_NAMES.length; i++) {
      int row = 0;
      int column = i;
      componentGrid.add(createComponentCell(VehicleComponentPaletteConstants.COMPONENT_NAMES[i]),
          column, row);
    }
    componentGrid.setGridLinesVisible(true);

    visualRoot = new ScrollPane(componentGrid);
    visualRoot.setFitToWidth(true);
    visualRoot.setFitToHeight(true);
    visualRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    visualRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    visualRoot.setPrefHeight(VehicleComponentPaletteConstants.PALETTE_PREF_HEIGHT);
    visualRoot.setMinHeight(VehicleComponentPaletteConstants.PALETTE_PREF_HEIGHT);
    visualRoot.setMaxHeight(VehicleComponentPaletteConstants.PALETTE_PREF_HEIGHT);
  }

  @Override
  public Node getVisualRoot() {
    return visualRoot;
  }

  private StackPane createComponentCell(String componentName) {
    Label label = new Label(componentName);
    label.setWrapText(true);
    label.setMaxWidth(VehicleComponentPaletteConstants.CELL_LABEL_WRAP_WIDTH);

    StackPane cell = new StackPane(label);
    cell.setAlignment(Pos.CENTER);
    cell.setPrefHeight(VehicleComponentPaletteConstants.CELL_PREF_HEIGHT);
    cell.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(cell, Priority.ALWAYS);
    GridPane.setFillWidth(cell, true);
    GridPane.setMargin(cell, new Insets(VehicleComponentPaletteConstants.CELL_INSET));
    return cell;
  }
}
