package oogasalad.view.authoring.internal.level;

import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.controller.authoring.internal.ToolSelectorImpl;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelSaverImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;

/**
 * Creates the canvas, world, controller, and panels that make up an authoring workspace.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
final class AuthoringWorkspaceFactory {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  Workspace create() {
    World world = new WorldImpl();
    LevelAttributes levelAttributes = new LevelAttributes();
    LevelAttributesPanel levelAttributesPanel = new LevelAttributesPanel(levelAttributes);
    TerrainPropertiesPanel terrainPropertiesPanel = new TerrainPropertiesPanel();
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl(world);
    canvas.setOnObjectSelected(terrainPropertiesPanel::show);
    terrainPropertiesPanel.setEditContext(world, canvas::refreshGrid);

    AuthoringControllerImpl controller = new AuthoringControllerImpl(
        world, new LevelLoaderImpl(), new LevelSaverImpl(canvas.getCellSize()),
        new GameLoaderImpl(), levelAttributes, new ToolSelectorImpl(), canvas);
    controller.initializeCanvasHandlers();

    ScrollPane canvasScrollPane = new ScrollPane(canvas.getVisualRoot());
    canvasScrollPane.setFitToWidth(false);
    canvasScrollPane.setFitToHeight(false);
    canvasScrollPane.setPannable(false);
    canvasScrollPane.getStyleClass().add("authoring-canvas-scroll");
    CanvasZoomPanHandler.Settings zoomPanSettings = new CanvasZoomPanHandler.Settings(
        d("edgePanThreshold"),
        d("edgePanSpeed"),
        d("zoomStep"));
    new CanvasZoomPanHandler(canvas, canvasScrollPane, zoomPanSettings).wireHandlers();

    StackPane canvasHost = new StackPane(canvasScrollPane);
    canvasHost.getStyleClass().add("authoring-canvas-host");
    VBox.setVgrow(canvasHost, javafx.scene.layout.Priority.ALWAYS);

    return new Workspace(world, levelAttributes, levelAttributesPanel, terrainPropertiesPanel,
        canvas, controller, canvasScrollPane, canvasHost);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }

  record Workspace(World world, LevelAttributes levelAttributes,
                   LevelAttributesPanel levelAttributesPanel,
                   TerrainPropertiesPanel terrainPropertiesPanel,
                   AuthoringCanvasImpl canvas, AuthoringControllerImpl controller,
                   ScrollPane canvasScrollPane, StackPane canvasHost) {

    void fitCanvasToViewport() {
      Platform.runLater(() -> {
        double viewportWidth = canvasScrollPane.getViewportBounds().getWidth();
        double canvasWidth = canvas.getVisualRoot().getBoundsInLocal().getWidth();
        if (viewportWidth > 0 && canvasWidth > 0) {
          canvas.setZoom(Math.min(1.0, viewportWidth / canvasWidth));
        }
      });
    }
  }
}
