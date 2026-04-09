package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen on which the player builds a vehicle
 * @author Iurii Beliaev
 */

public class VehicleBuilderAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.VehicleBuilderAppScreen");

  public VehicleBuilderAppScreen(Consumer<Outcome> onOutcome) {
    super(onOutcome);
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

    GridPane componentGrid = createComponentGrid();
    componentGrid.getStyleClass().add("component-grid");
    componentGrid.setAlignment(Pos.CENTER);

    container.getChildren().addAll(title, componentGrid);

    return container;
  }

  private GridPane createComponentGrid() {
    GridPane componentGrid = new GridPane();
    componentGrid.getStyleClass().add("component-grid");
    componentGrid.setAlignment(Pos.CENTER);

    int cols = (int) d("componentGrid.columns");
    int rows = (int) d("componentGrid.rows");

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        StackPane componentCell = new StackPane();
        componentCell.getStyleClass().add("component-cell");
        componentGrid.add(componentCell, col, row);
      }
    }

    return componentGrid;
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
        grid.add(cell, col, row);
      }
    }

    return grid;
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
