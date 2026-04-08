package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import oogasalad.view.authoring.api.ComponentPallete;

/**
 * Scrollable vehicle-component palette laid out in rows of equal-width cells.
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");
  private static final ResourceBundle SCREEN_PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private final ScrollPane visualRoot;

  public VehicleComponentScrollPalette() {
    int componentsPerRow = Integer.parseInt(PROPS.getString("componentsPerRow"));
    double contentPadding = Double.parseDouble(PROPS.getString("contentPadding"));
    String[] componentNames = PROPS.getString("componentNames").split(",");

    GridPane componentGrid = new GridPane();
    componentGrid.setMaxWidth(Double.MAX_VALUE);
    componentGrid.setPadding(new Insets(contentPadding));

    for (int i = 0; i < componentsPerRow; i++) {
      ColumnConstraints constraints = new ColumnConstraints();
      constraints.setPercentWidth(100.0 / componentsPerRow);
      constraints.setHgrow(Priority.ALWAYS);
      componentGrid.getColumnConstraints().add(constraints);
    }

    for (int i = 0; i < componentNames.length; i++) {
      int row = i / componentsPerRow;
      int column = i % componentsPerRow;
      componentGrid.add(createComponentCell(componentNames[i].trim()), column, row);
    }

    double paletteHeight = Double.parseDouble(SCREEN_PROPS.getString("bottomRegionHeight"));
    visualRoot = new ScrollPane(componentGrid);
    componentGrid.setGridLinesVisible(true);
    visualRoot.setFitToWidth(true);
    visualRoot.setFitToHeight(false);
    visualRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    visualRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    visualRoot.setPrefHeight(paletteHeight);
    visualRoot.setMinHeight(paletteHeight);
    visualRoot.setMaxHeight(paletteHeight);
    visualRoot.getStyleClass().add("authoring-vehicle-palette");
  }

  @Override
  public Node getVisualRoot() {
    return visualRoot;
  }

  private StackPane createComponentCell(String componentName) {
    Label label = new Label(componentName);
    label.setWrapText(true);
    label.setMaxWidth(Double.parseDouble(PROPS.getString("cellLabelWrapWidth")));

    StackPane cell = new StackPane(label);
    cell.setAlignment(Pos.CENTER);
    cell.setPrefHeight(Double.parseDouble(PROPS.getString("cellPrefHeight")));
    cell.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(cell, Priority.ALWAYS);
    GridPane.setFillWidth(cell, true);
    GridPane.setMargin(cell, new Insets(Double.parseDouble(PROPS.getString("cellInset"))));
    return cell;
  }
}
