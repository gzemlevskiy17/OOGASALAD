package oogasalad.view.authoring.internal.level;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Builds JavaFX cells for the selectable vehicle component palette.
 *
 * @author Emilio Garcia
 */
final class VehicleComponentCellFactory {

  private static final String CELL_STYLE_CLASS = "vehicle-component-cell";
  private static final String SELECTED_STYLE_CLASS = "vehicle-component-cell-selected";
  private static final String UNSELECTED_CELL_STYLE = """
      -fx-border-color: #707070;
      -fx-border-width: 1;
      -fx-background-color: white;
      """;
  private static final String SELECTED_CELL_STYLE = """
      -fx-border-color: #e67e22;
      -fx-border-width: 3;
      -fx-background-color: white;
      """;

  private final VehiclePaletteConfig config;
  private final SpriteCache spriteCache;

  VehicleComponentCellFactory(VehiclePaletteConfig config, SpriteCache spriteCache) {
    this.config = config;
    this.spriteCache = spriteCache;
  }

  StackPane createCell(SelectedVehicleComponent component,
      Consumer<SelectedVehicleComponent> selectionHandler) {
    VBox cellContent = new VBox(2);
    cellContent.setAlignment(Pos.CENTER);
    config.spritePathFor(component.id())
        .map(spriteCache::get)
        .ifPresent(image -> cellContent.getChildren().add(iconFor(image)));
    cellContent.getChildren().add(labelFor(component.displayName()));

    StackPane cell = new StackPane(cellContent);
    cell.setId("vehicle-component-cell-" + stableId(component.id()));
    cell.getStyleClass().add(CELL_STYLE_CLASS);
    cell.setAlignment(Pos.CENTER);
    cell.setMinWidth(config.cellWidth());
    cell.setPrefWidth(config.cellWidth());
    cell.setPrefHeight(config.cellPrefHeight());
    cell.setPadding(new Insets(config.cellInset()));
    setSelected(cell, false);
    cell.setOnMouseClicked(event -> selectionHandler.accept(component));
    HBox.setHgrow(cell, Priority.NEVER);
    return cell;
  }

  void setSelected(StackPane cell, boolean selected) {
    if (selected) {
      if (!cell.getStyleClass().contains(SELECTED_STYLE_CLASS)) {
        cell.getStyleClass().add(SELECTED_STYLE_CLASS);
      }
      cell.setStyle(SELECTED_CELL_STYLE);
      return;
    }
    cell.getStyleClass().remove(SELECTED_STYLE_CLASS);
    cell.setStyle(UNSELECTED_CELL_STYLE);
  }

  private ImageView iconFor(Image image) {
    ImageView icon = new ImageView(image);
    icon.setFitWidth(config.cellIconSize());
    icon.setFitHeight(config.cellIconSize());
    icon.setPreserveRatio(true);
    return icon;
  }

  private Label labelFor(String displayName) {
    Label label = new Label(displayName);
    label.setWrapText(true);
    label.setMaxWidth(config.cellLabelWrapWidth());
    label.setAlignment(Pos.CENTER);
    label.setTextAlignment(TextAlignment.CENTER);
    label.setStyle("-fx-font-size: 10px;");
    return label;
  }

  private String stableId(String componentId) {
    String stable = componentId.replaceAll("[^A-Za-z0-9_-]", "");
    return stable.isBlank() ? "component" : stable;
  }
}
