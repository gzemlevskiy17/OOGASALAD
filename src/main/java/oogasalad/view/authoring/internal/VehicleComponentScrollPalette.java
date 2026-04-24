package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Scrollable vehicle-component palette laid out in rows of equal-width cells.
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private static final ResourceBundle PALETTE_CONFIG =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");
  private static final ResourceBundle SCREEN_CONFIG =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private final ScrollPane visualRoot;
  private final SpriteCache spriteCache = new SpriteCache();

  public VehicleComponentScrollPalette() {
    int componentsPerRow = Integer.parseInt(PALETTE_CONFIG.getString("componentsPerRow"));
    double contentPadding = Double.parseDouble(PALETTE_CONFIG.getString("contentPadding"));
    String[] componentNames = PALETTE_CONFIG.getString("componentNames").split(",");

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

    double paletteHeight = Double.parseDouble(SCREEN_CONFIG.getString("bottomRegionHeight"));
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
    label.setMaxWidth(Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth")));

    VBox cellContent = new VBox(4);
    cellContent.setAlignment(Pos.CENTER);

    String key = componentName.replace(" ", "");
    String spriteKey = key + ".sprite";
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
    cell.setPrefHeight(Double.parseDouble(PALETTE_CONFIG.getString("cellPrefHeight")));
    cell.setMaxWidth(Double.MAX_VALUE);
    GridPane.setHgrow(cell, Priority.ALWAYS);
    GridPane.setFillWidth(cell, true);
    GridPane.setMargin(cell, new Insets(Double.parseDouble(PALETTE_CONFIG.getString("cellInset"))));
    return cell;
  }
}
