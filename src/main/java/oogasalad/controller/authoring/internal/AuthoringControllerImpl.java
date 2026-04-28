package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import oogasalad.controller.authoring.api.AuthoringController;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameLoader;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates the authoring environment: composes input handlers and services, exposes the
 * level-editing API. Mouse routing, persistence, and spawn invariants live in collaborator
 * classes; this class is the thin glue.
 *
 * @author Paulina Vvedenskaya
 */
public class AuthoringControllerImpl implements AuthoringController {

  private static final Logger LOG = LogManager.getLogger(AuthoringControllerImpl.class);

  private final World world;
  private final ToolSelector toolSelector;
  private final AuthoringCanvas canvas;
  private final CanvasActionHistory history;
  private final SplineDraftController splineDraft;
  private final SpawnPointService spawnPointService;
  private final GoalLineService goalLineService;
  private final AuthoringPersistenceService persistence;

  private AuthoringPlaceable selectedPlaceable;

  public AuthoringControllerImpl(World world, LevelLoader loader, LevelSaver saver,
      GameLoader gameLoader, LevelAttributes levelAttributes, ToolSelector toolSelector,
      AuthoringCanvas canvas) {
    this.world = world;
    this.toolSelector = toolSelector;
    this.canvas = canvas;
    this.history = new CanvasActionHistory();
    this.splineDraft = new SplineDraftController(world, canvas);
    this.spawnPointService = new SpawnPointService(world);
    this.goalLineService = new GoalLineService(world);
    this.persistence = new AuthoringPersistenceService(
        loader, saver, gameLoader, world, levelAttributes, spawnPointService, history, canvas);
    spawnPointService.getSpawnPoint();
  }

  /** @return the spline-draft controller, exposed so the UI shell can wire resolution edits. */
  public SplineDraftController getSplineDraft() {
    return splineDraft;
  }

  public void setActiveTool(ToolSelector.ToolType type) {
    if (toolSelector.getActiveTool() == ToolType.SPLINE && type != ToolType.SPLINE) {
      splineDraft.cancelIfDrafting();
    }
    toolSelector.setActiveTool(type);
    LOG.debug("Active tool set to {}", type);
  }

  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    if (placeable != null) {
      this.selectedPlaceable = placeable;
    }
  }

  public void setSplineTerrainStyle(AuthoringPlaceable placeable) {
    if (placeable != null) {
      splineDraft.setTerrainStyle(placeable.getDisplayName(), placeable.getSprite(),
          placeable.isTiled(), placeable.getFriction(), placeable.getColor(),
          placeable.getTrackColor());
    }
  }

  @Override
  public void initializeCanvasHandlers() {
    Node root = canvas.getVisualRoot();
    new SpawnDragInputHandler(canvas, spawnPointService, toolSelector).register(root);
    new GoalLineDragInputHandler(canvas, goalLineService).register(root);
    new SplineInputHandler(canvas, toolSelector, splineDraft).register(root);
    new HoverPreviewInputHandler(canvas, toolSelector).register(root);
    new PaletteDropInputHandler(canvas, (col, row) -> placeFromPaletteDrop(col, row)).register(root);
    new ClickPlaceInputHandler(canvas, toolSelector,
        (col, row) -> placeFromPaletteDrop(col, row)).register(root);
  }

  @Override
  public void saveLevel(String fileName) {
    commitSplineDraftBeforeSave();
    persistence.save(fileName);
  }

  /**
   * Commits the in-progress spline draft into the world and records the action so it can be
   * undone. Call this from the UI commit button instead of accessing the draft directly.
   */
  public void commitSpline() {
    commitSplineDraftBeforeSave();
  }

  private void commitSplineDraftBeforeSave() {
    TerrainSpline committed = splineDraft.commit();
    if (committed != null) {
      history.record(CanvasAction.place(committed));
      canvas.refreshGrid();
      LOG.debug("Committed spline '{}'", committed.getDisplayName());
    }
  }

  @Override
  public void loadLevel(String fileName) {
    persistence.load(fileName);
  }

  @Override
  public void undo() {
    history.undo().ifPresent(batch -> {
      for (int i = batch.size() - 1; i >= 0; i--) {
        applyInverse(batch.get(i));
      }
      canvas.refreshGrid();
      LOG.debug("Undo applied: {} action(s)", batch.size());
    });
  }

  @Override
  public void redo() {
    history.redo().ifPresent(batch -> {
      for (CanvasAction action : batch) {
        applyAction(action);
      }
      canvas.refreshGrid();
      LOG.debug("Redo applied: {} action(s)", batch.size());
    });
  }

  @Override
  public void clearHistory() {
    history.clear();
    LOG.debug("Undo/redo history cleared");
  }

  @Override
  public void clearLevel() {
    world.clear();
    history.clear();
    spawnPointService.getSpawnPoint();
    canvas.refreshGrid();
    LOG.info("Level cleared");
  }

  GameObject placeSelectedObject(int col, int row) {
    if (selectedPlaceable == null) {
      return null;
    }
    double cellSize = canvas.getCellSize();
    AuthoringPlaceable placedObject = selectedPlaceable.createAt(col, row, cellSize);
    world.addObject((GameObject) placedObject);
    history.record(CanvasAction.place(placedObject));
    LOG.debug("Placed '{}' at col={} row={}", placedObject.getDisplayName(), col, row);
    return (GameObject) placedObject;
  }

  private void placeFromPaletteDrop(int col, int row) {
    history.beginBatch();
    try {
      placeSelectedObject(col, row);
    } finally {
      history.endBatch();
    }
    canvas.refreshGrid();
  }

  private void applyAction(CanvasAction action) {
    switch (action.type()) {
      case PLACE -> placeFromAction(action);
      case REMOVE -> removeFromAction(action);
    }
  }

  private void applyInverse(CanvasAction action) {
    switch (action.type()) {
      case PLACE -> removeFromAction(action);
      case REMOVE -> placeFromAction(action);
    }
  }

  private void placeFromAction(CanvasAction action) {
    if (action.object() != null) {
      world.addObject((GameObject) action.object());
      return;
    }
    TerrainBlock block = new TerrainBlock(
        action.displayName(), action.col(), action.row(),
        action.width(), action.height(),
        action.sprite(), action.tiled(),
        action.friction(), action.color(), action.restitution());
    world.addObject(block);
  }

  private void removeFromAction(CanvasAction action) {
    if (action.object() != null) {
      world.removeObject((GameObject) action.object());
      return;
    }
    removePlaceableAt(action.col(), action.row());
  }

  private void removePlaceableAt(int col, int row) {
    AuthoringPlaceable target = findPlaceableAt(col, row);
    if (target != null) {
      world.removeObject((GameObject) target);
    }
  }

  private AuthoringPlaceable findPlaceableAt(int col, int row) {
    List<AuthoringPlaceable> placeables = collectPlaceables();
    OccupancyGrid grid = new OccupancyGrid(placeables, canvas.getGridCols(), canvas.getGridRows());
    return grid.getObjectAt(col, row);
  }

  private List<AuthoringPlaceable> collectPlaceables() {
    List<AuthoringPlaceable> placeables = new ArrayList<>();
    for (GameObject object : world.getObjects()) {
      if (object instanceof AuthoringPlaceable placeable) {
        placeables.add(placeable);
      }
    }
    return placeables;
  }
}
