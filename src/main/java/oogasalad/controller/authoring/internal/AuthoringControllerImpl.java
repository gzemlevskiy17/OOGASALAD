package oogasalad.controller.authoring.internal;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import oogasalad.controller.authoring.api.AuthoringController;
import oogasalad.controller.authoring.api.AuthoringPlacementException;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
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
 *   ERASE → snap mouse position to grid cell, remove any placed object whose
 *           bounds contain that cell.
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
  private final LevelAttributes levelAttributes;
  private final ToolSelector toolSelector;
  private final AuthoringCanvas canvas;
  private final PlacementValidator placementValidator;
  private final CanvasActionHistory history;

  private AuthoringPlaceable selectedPlaceable;
  private Consumer<String> placementErrorHandler = message -> { };
  private Consumer<ToolType> toolChangeListener;
  private int lastDragCol = -1;
  private int lastDragRow = -1;

  public AuthoringControllerImpl(World world, LevelLoader loader, LevelSaver saver,
      LevelAttributes levelAttributes, ToolSelector toolSelector, AuthoringCanvas canvas) {
    this.world = world;
    this.loader = loader;
    this.saver = saver;
    this.levelAttributes = levelAttributes;
    this.toolSelector = toolSelector;
    this.canvas = canvas;
    this.placementValidator = new PlacementValidator();
    this.history = new CanvasActionHistory();
    double cellSize = canvas.getCellSize();
    this.selectedPlaceable = new TerrainPlaceable(
        cellSize, cellSize, "#8B4513", "assets/terrain/grass.png", true, 0.6);
  }

  public void setActiveTool(ToolSelector.ToolType type) {
    toolSelector.setActiveTool(type);
  }

  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    if (placeable != null) {
      this.selectedPlaceable = placeable;
    }
  }

  public void setToolChangeListener(Consumer<ToolType> listener) {
    this.toolChangeListener = listener;
  }

  public void setPlacementErrorHandler(Consumer<String> placementErrorHandler) {
    if (placementErrorHandler != null) {
      this.placementErrorHandler = placementErrorHandler;
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

    // Batch lifecycle runs as capture-phase filters so it fires even when a
    // child (e.g. a placed block's Rectangle) consumes the MOUSE_PRESSED event
    // for object selection. Without this, drags starting on top of an
    // existing block would record each cell as its own undo step.
    root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        history.beginBatch();
      }
    });
    root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        history.endBatch();
      }
    });

    root.setOnMousePressed(e -> startStrokeAt(e.getX(), e.getY()));
    root.setOnMouseDragged(e -> extendStrokeTo(e.getX(), e.getY()));
    root.setOnMouseReleased(e -> {
      if (toolSelector.getActiveTool() == ToolType.PAINT) {
        toolSelector.setActiveTool(ToolType.DRAW);
        if (toolChangeListener != null) {
          toolChangeListener.accept(ToolType.DRAW);
        }
      }
    });

    // Drag-and-drop from a palette item (separate event stream from the
    // mouse-press paint stroke above). ComponentPaletteImpl starts a Dragboard
    // gesture in setOnDragDetected; the handlers below preview and place the
    // dropped block at the cursor cell.
    root.setOnDragOver(e -> {
      if (e.getGestureSource() != root && e.getDragboard().hasString()) {
        e.acceptTransferModes(TransferMode.COPY);
        int col = (int) Math.floor(e.getX() / cellSize);
        int row = (int) Math.floor(e.getY() / cellSize);
        canvas.showHoverPreview(col, row);
      }
      e.consume();
    });

    root.setOnDragExited(e -> {
      canvas.clearHoverPreview();
      e.consume();
    });

    root.setOnDragDropped(e -> {
      boolean placed = false;
      if (e.getDragboard().hasString()) {
        int col = (int) Math.floor(e.getX() / cellSize);
        int row = (int) Math.floor(e.getY() / cellSize);
        if (canvas.isWithinBounds(col, row)) {
          history.beginBatch();
          try {
            handleCellInteraction(col, row);
          } finally {
            history.endBatch();
          }
          placed = true;
        }
      }
      e.setDropCompleted(placed);
      e.consume();
    });
  }

  private void startStrokeAt(double x, double y) {
    double cellSize = canvas.getCellSize();
    int col = (int) Math.floor(x / cellSize);
    int row = (int) Math.floor(y / cellSize);
    if (!canvas.isWithinBounds(col, row)) {
      return;
    }
    handleCellInteraction(col, row);
    lastDragCol = col;
    lastDragRow = row;
  }

  private void extendStrokeTo(double x, double y) {
    if (toolSelector.getActiveTool() == ToolType.PAINT) {
      return;
    }
    double cellSize = canvas.getCellSize();
    int col = (int) Math.floor(x / cellSize);
    int row = (int) Math.floor(y / cellSize);
    if (!canvas.isWithinBounds(col, row)) {
      return;
    }
    int spanCols = selectedPlaceable.colSpan(cellSize);
    int spanRows = selectedPlaceable.rowSpan(cellSize);
    int[] lastPlaced = fillLine(lastDragCol, lastDragRow, col, row, spanCols, spanRows);
    lastDragCol = lastPlaced[0];
    lastDragRow = lastPlaced[1];
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
    try {
      if (toolSelector.getActiveTool() == ToolType.DRAW) {
        placeSelectedObject(col, row);
      } else if (toolSelector.getActiveTool() == ToolType.ERASE) {
        eraseAt(col, row);
      } else if (toolSelector.getActiveTool() == ToolType.PAINT) {
        floodFillAt(col, row);
      }
    } catch (AuthoringPlacementException e) {
      placementErrorHandler.accept(e.getMessage());
    }
    canvas.refreshGrid();
  }

  private void eraseAt(int col, int row) {
    OccupancyGrid grid = buildOccupancyGrid();
    AuthoringPlaceable target = grid.getObjectAt(col, row);
    if (target != null) {
      history.record(CanvasAction.remove(target));
      world.removeObject((GameObject) target);
    }
  }

  /**
   * Interpolates a line between two grid cells calling handleCellInteraction for each cell along the line.
   */
  private int[] fillLine(int x0, int y0, int x1, int y1, int spanCols, int spanRows) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int lastPlacedCol = x0;
    int lastPlacedRow = y0;
    boolean firstPoint = true;
    while (true) {
      boolean reachedNextPlacement =
          Math.abs(x0 - lastPlacedCol) >= spanCols || Math.abs(y0 - lastPlacedRow) >= spanRows;
      if (!firstPoint && reachedNextPlacement) {
        handleCellInteraction(x0, y0);
        lastPlacedCol = x0;
        lastPlacedRow = y0;
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
    return new int[] {lastPlacedCol, lastPlacedRow};
  }

  @Override
  public void saveLevel(String fileName) {
    saver.saveLevel(fileName, world);
    LOG.info("Saved level to {}", fileName);
  }

  @Override
  public void loadLevel(String fileName) {
    world.clear();
    loader.loadLevel(fileName, world, null);
    history.clear();
    canvas.refreshGrid();
    LOG.info("Loaded level from {}", fileName);
  }

  @Override
  public void undo() {
    history.undo().ifPresent(batch -> {
      for (int i = batch.size() - 1; i >= 0; i--) {
        applyInverse(batch.get(i));
      }
      canvas.refreshGrid();
    });
  }

  @Override
  public void redo() {
    history.redo().ifPresent(batch -> {
      for (CanvasAction action : batch) {
        applyAction(action);
      }
      canvas.refreshGrid();
    });
  }

  @Override
  public void clearHistory() {
    history.clear();
  }

  @Override
  public void clearLevel() {
    world.clear();
    history.clear();
    canvas.refreshGrid();
  }

  private void applyAction(CanvasAction action) {
    switch (action.type()) {
      case PLACE -> placeFromAction(action);
      case REMOVE -> removePlaceableAt(action.col(), action.row());
    }
  }

  private void applyInverse(CanvasAction action) {
    switch (action.type()) {
      case PLACE -> removePlaceableAt(action.col(), action.row());
      case REMOVE -> placeFromAction(action);
    }
  }

  private void placeFromAction(CanvasAction action) {
    TerrainBlock block = new TerrainBlock(
        action.col(), action.row(),
        action.width(), action.height(),
        action.sprite(), action.tiled(),
        action.friction(), action.color());
    world.addObject(block);
  }

  private void removePlaceableAt(int col, int row) {
    AuthoringPlaceable target = buildOccupancyGrid().getObjectAt(col, row);
    if (target != null) {
      world.removeObject((GameObject) target);
    }
  }

  GameObject placeSelectedObject(int col, int row) {
    double cellSize = canvas.getCellSize();
    AuthoringPlaceable placedObject = selectedPlaceable.createAt(col, row, cellSize);
    OccupancyGrid grid = buildOccupancyGrid();
    if (placementValidator.overlapsExistingPlacement(placedObject, grid)) {
      autoFillEnclosed(col, row, grid);
      return null;
    }
    world.addObject((GameObject) placedObject);
    history.record(CanvasAction.place(placedObject));
    return (GameObject) placedObject;
  }

  private void floodFillAt(int col, int row) {
    double cellSize = canvas.getCellSize();
    OccupancyGrid grid = buildOccupancyGrid();
    FillExecutor executor = new FillExecutor();
    List<int[]> cellsToFill = executor.computeFillRegion(grid, col, row);

    TerrainPlaceable unitPlaceable = new TerrainPlaceable(
        cellSize, cellSize,
        selectedPlaceable.getColor(),
        selectedPlaceable.getSprite(),
        selectedPlaceable.isTiled(),
        selectedPlaceable.getFriction());

    history.beginBatch();
    try {
      for (int[] cell : cellsToFill) {
        AuthoringPlaceable placed = unitPlaceable.createAt(cell[0], cell[1], cellSize);
        world.addObject((GameObject) placed);
        history.record(CanvasAction.place(placed));
      }
    } finally {
      history.endBatch();
    }
  }

  private static final int[][] NEIGHBOR_DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private void autoFillEnclosed(int col, int row, OccupancyGrid grid) {
    FillExecutor executor = new FillExecutor();
    FillExecutor.FillResult best = null;

    for (int[] dir : NEIGHBOR_DIRS) {
      int nc = col + dir[0];
      int nr = row + dir[1];
      if (!grid.isInBounds(nc, nr) || grid.isOccupied(nc, nr)) {
        continue;
      }
      FillExecutor.FillResult result = executor.computeEnclosedRegion(grid, nc, nr);
      if (result.status() == FillExecutor.FillStatus.FILLED) {
        if (best == null || result.cells().size() < best.cells().size()) {
          best = result;
        }
      }
    }

    if (best == null) {
      return;
    }

    double cellSize = canvas.getCellSize();
    TerrainPlaceable unitPlaceable = new TerrainPlaceable(
        cellSize, cellSize,
        selectedPlaceable.getColor(),
        selectedPlaceable.getSprite(),
        selectedPlaceable.isTiled(),
        selectedPlaceable.getFriction());
    history.beginBatch();
    try {
      for (int[] cell : best.cells()) {
        AuthoringPlaceable placed = unitPlaceable.createAt(cell[0], cell[1], cellSize);
        world.addObject((GameObject) placed);
        history.record(CanvasAction.place(placed));
      }
    } finally {
      history.endBatch();
    }
  }

  private OccupancyGrid buildOccupancyGrid() {
    List<AuthoringPlaceable> placeables = world.getObjects().stream()
        .filter(AuthoringPlaceable.class::isInstance)
        .map(AuthoringPlaceable.class::cast)
        .toList();
    return new OccupancyGrid(placeables, canvas.getGridCols(), canvas.getGridRows());
  }
}
