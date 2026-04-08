package oogasalad.controller.authoring.internal;

import java.util.ResourceBundle;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.AuthoringController;
import oogasalad.controller.authoring.api.AuthoringPlacementException;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import oogasalad.view.authoring.internal.TerrainPlaceable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates the level editor: translates grid interactions into model changes,
 * delegates save/load to the data layer, and triggers canvas redraws.
 *
 * Grid interaction rules:
 *   DRAW  → snap mouse position to grid cell, place a 1x1 TerrainBlock there.
 *   ERASE → requires World.removeObject() which is not yet on the World interface.
 *           Flagged as TODO — pending discussion with World owner (Xuye).
 *
 * NOTE: loadLevel() currently appends to the existing World rather than clearing
 * it first. A World.clear() method is needed for proper reload behavior.
 * Flagged as TODO — pending discussion with World owner (Xuye).
 *
 * Collaborators: World (model state via interface only),
 *                LevelLoaderImpl (populates world from file),
 *                LevelSaverImpl (persists world to file),
 *                ToolSelectorImpl (current editor tool),
 *                AuthoringCanvasImpl (refreshed after each model change).
 */
public class AuthoringControllerImpl implements AuthoringController {

  private static final Logger LOG = LogManager.getLogger(AuthoringControllerImpl.class);
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle("oogasalad.controller.authoring.AuthoringController");

  private final World world;
  private final LevelLoader loader;
  private final LevelSaver saver;
  private final GameConfig config;
  private final ToolSelector toolSelector;
  private final AuthoringCanvas canvas;
  private final PlacementValidator placementValidator;

  private AuthoringPlaceable selectedPlaceable;
  private int lastDragCol = -1;
  private int lastDragRow = -1;

  public AuthoringControllerImpl(World world, LevelLoader loader, LevelSaver saver,
      GameConfig config, ToolSelector toolSelector, AuthoringCanvas canvas) {
    this.world = world;
    this.loader = loader;
    this.saver = saver;
    this.config = config;
    this.toolSelector = toolSelector;
    this.canvas = canvas;
    this.placementValidator = new PlacementValidator();
    double cellSize = canvas.getCellSize();
    this.selectedPlaceable = new TerrainPlaceable(
        cellSize, cellSize, "#8B4513", "assets/terrain/grass.png", true, 0.6);
  }

  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    if (placeable != null) {
      this.selectedPlaceable = placeable;
    }
  }

  @Override
  public void initializeCanvasHandlers() {
    Node root = canvas.getVisualRoot();
    double cellSize = canvas.getCellSize();

    root.setOnMouseMoved(e -> {
      int col = (int) Math.floor(e.getX() / cellSize);
      int row = (int) Math.floor(e.getY() / cellSize);
      canvas.showHoverPreview(col, row);
    });

    root.setOnMouseExited(e -> canvas.clearHoverPreview());

    root.setOnMousePressed(e -> {
      int col = (int) Math.floor(e.getX() / cellSize);
      int row = (int) Math.floor(e.getY() / cellSize);
      if (!canvas.isWithinBounds(col, row)) {
        return;
      }
      lastDragCol = col;
      lastDragRow = row;
      handleCellInteraction(col, row);
    });

    root.setOnMouseDragged(e -> {
      int col = (int) Math.floor(e.getX() / cellSize);
      int row = (int) Math.floor(e.getY() / cellSize);
      if (!canvas.isWithinBounds(col, row)) {
        return;
      }
      fillLine(lastDragCol, lastDragRow, col, row);
      lastDragCol = col;
      lastDragRow = row;
    });
  }

  /**
   * Translates a mouse position into a grid cell and applies the active tool.
   * Called on both click and drag events from the canvas.
   */
  @Override
  public void handleGridInteraction(MouseEvent event) {
    handleGridInteraction(event.getX(), event.getY());
  }

  /**
   * Core grid interaction logic. Package-private so tests can invoke it
   * directly without needing to construct a JavaFX MouseEvent.
   */
  void handleGridInteraction(double x, double y) {
    double cellSize = canvas.getCellSize();
    int col = (int) Math.floor(x / cellSize);
    int row = (int) Math.floor(y / cellSize);
    if (canvas.isWithinBounds(col, row)) {
      handleCellInteraction(col, row);
    }
  }

  /**
   * Applies the active tool at the given grid cell. Package-private so tests
   * can invoke it directly.
   */
  void handleCellInteraction(int col, int row) {
    if (toolSelector.getActiveTool() == ToolType.DRAW) {
      placeSelectedObject(col, row);
    } else if (toolSelector.getActiveTool() == ToolType.ERASE) {
      // TODO: requires World.removeObject() — flag to Xuye to add to World interface.
      LOG.warn("Erase tool not yet supported: World.removeObject() pending API addition.");
    }
    canvas.refreshGrid();
  }

  /**
   * Interpolates a line between two grid cells calling handleCellInteraction for each cell along the line.
   */
  private void fillLine(int x0, int y0, int x1, int y1) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    boolean firstPoint = true;
    while (true) {
      if (!firstPoint) {
        handleCellInteraction(x0, y0);
      }
      if (x0 == x1 && y0 == y1) {
        break;
      }
      firstPoint = false;
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x0 += sx;
      }
      if (e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
  }

  @Override
  public void saveLevel(String fileName) {
    saver.saveLevel(fileName, world);
    LOG.info("Saved level to {}", fileName);
  }

  @Override
  public void loadLevel(String fileName) {
    // TODO: world should be cleared before loading — requires World.clear() from Xuye.
    loader.loadLevel(fileName, world, config);
    canvas.refreshGrid();
    LOG.info("Loaded level from {}", fileName);
  }

  GameObject placeSelectedObject(int col, int row) {
    double cellSize = canvas.getCellSize();
    AuthoringPlaceable placedObject = selectedPlaceable.createAt(col, row, cellSize);
    if (placementValidator.overlapsExistingPlacement(
        placedObject,
        world.getObjects().stream().map(AuthoringPlaceable.class::cast).toList())) {
      String message = MESSAGES.getString("placement.overlap");
      LOG.warn(message);
      throw new AuthoringPlacementException(message);
    }
    world.addObject((GameObject) placedObject);
    return (GameObject) placedObject;
  }
}
