package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen on which the player builds a vehicle
 * @author Iurii Beliaev
 */

public class VehicleBuilderAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.VehicleBuilderAppScreen");

  public VehicleBuilderAppScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(getClass().getResource("/oogasalad/view/player/vehicle-builder.css").toExternalForm());
    root.getStyleClass().add("screen-root");

    VBox mainContent = new VBox(d("main.content.spacing"));
    mainContent.setAlignment(Pos.CENTER);

    // Assembling the regions
    mainContent.getChildren().addAll(
        createGridRegion(),
        createComponentSection(),
        createStartLevelRegion()
    );

    Node backButton = createBackButtonRegion();

    root.setCenter(mainContent);
    root.setBottom(backButton);

    return root;
  }

  private Node createStartLevelRegion() {
    Button startButton = new Button(s("start.button.text"));
    startButton.getStyleClass().add("start-button");
    startButton.setOnAction(event -> {
      screenManager.switchScreen(ScreenFactory.GAMEPLAY_APP_SCREEN_KEY, null);
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

    // Read grid dimensions from the properties file - later to be replaced by the passed values
    int cols = (int) d("componentGrid.columns");
    int rows = (int) d("componentGrid.rows");

    // Populate the grid with empty cells initially
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        StackPane componentCell = new StackPane();
        componentCell.getStyleClass().add("component-cell");

        // Add the cell to the grid at the specified column and row
        componentGrid.add(componentCell, col, row);
      }
    }

    return componentGrid;
  }

  private GridPane createGridRegion() {
    GridPane grid = new GridPane();
    grid.getStyleClass().add("vehicle-grid");
    grid.setAlignment(Pos.CENTER);

    // Read grid dimensions from the properties file - later to be replaced by the passed values
    int cols = (int) d("grid.columns");
    int rows = (int) d("grid.rows");

    // Populate the grid with empty cells initially
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("grid-cell");

        // Add the cell to the grid at the specified column and row
        grid.add(cell, col, row);
      }
    }

    return grid;
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> screenManager.switchScreen(ScreenFactory.PLAYER_ENTRY_APP_SCREEN_KEY, null));

    StackPane container = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("nav-region");
    return container;
  }

  private String s(String key) { return PROPS.getString(key); }

  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}