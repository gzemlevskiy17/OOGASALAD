package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
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

/**
 * Scrollable vehicle-component palette displayed as one long horizontal row of component cells.
 *
 * @author Emilio Garcia
 */
public class VehicleComponentScrollPalette implements ComponentPallete {

  private static final ResourceBundle PALETTE_CONFIG =
      ResourceBundle.getBundle("oogasalad.view.authoring.VehicleComponentPalette");

  private final ScrollPane visualRoot;
  private final SpriteCache spriteCache = new SpriteCache();

  /**
   * Creates the vehicle component palette view.
   *
   * @author Emilio Garcia
   */
  public VehicleComponentScrollPalette() {
    double contentPadding = Double.parseDouble(PALETTE_CONFIG.getString("contentPadding"));
    String[] componentNames = PALETTE_CONFIG.getString("componentNames").split(",");

    HBox paletteContent = new HBox(Double.parseDouble(PALETTE_CONFIG.getString("itemSpacing")));
    paletteContent.setPadding(new Insets(contentPadding));
    paletteContent.setAlignment(Pos.CENTER);
    paletteContent.getStyleClass().add("authoring-level-palette");

    for (String componentName : componentNames) {
      if (!componentName.isBlank()) {
        paletteContent.getChildren().add(createComponentCell(componentName.trim()));
      }
    }

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
  }

  @Override
  public Node getVisualRoot() {
    return visualRoot;
  }

  private StackPane createComponentCell(String componentName) {
    Label label = new Label(componentName);
    label.setWrapText(true);
    label.setMaxWidth(Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth")));
    label.setAlignment(Pos.CENTER);
    label.setTextAlignment(TextAlignment.CENTER);
    label.setStyle("-fx-font-size: 10px;");

    VBox cellContent = new VBox(2);
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
    double cellWidth = Double.parseDouble(PALETTE_CONFIG.getString("cellLabelWrapWidth"))
        + 2 * Double.parseDouble(PALETTE_CONFIG.getString("cellInset"));
    cell.setMinWidth(cellWidth);
    cell.setPrefWidth(cellWidth);
    cell.setPrefHeight(Double.parseDouble(PALETTE_CONFIG.getString("cellPrefHeight")));
    cell.setPadding(new Insets(Double.parseDouble(PALETTE_CONFIG.getString("cellInset"))));
    cell.setStyle("""
        -fx-border-color: #707070;
        -fx-border-width: 1;
        -fx-background-color: white;
        """);
    HBox.setHgrow(cell, Priority.NEVER);
    return cell;
  }
}
