package oogasalad.controller.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import oogasalad.controller.authoring.api.AuthoringController;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Coordinates the authoring environment: composes input handlers and services, exposes the
 * level-editing API. Mouse routing, persistence, and spawn invariants live in collaborator
 * classes; this class is the thin glue.
 *
 * @author Paulina Vvedenskaya
 */
public class AuthoringControllerImpl implements AuthoringController {

  private final World world;
  private final ToolSelector toolSelector;
  private final AuthoringCanvas canvas;
  private final CanvasActionHistory history;
  private final SplineDraftController splineDraft;
  private final SpawnPointService spawnPointService;
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
  }

  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    if (placeable != null) {
      this.selectedPlaceable = placeable;
    }
  }

  @Override
  public void initializeCanvasHandlers() {
    Node root = canvas.getVisualRoot();
    new SpawnDragInputHandler(canvas, spawnPointService, toolSelector).register(root);
    new SplineInputHandler(canvas, toolSelector, splineDraft).register(root);
    new HoverPreviewInputHandler(canvas, toolSelector).register(root);
    new PaletteDropInputHandler(canvas, (col, row) -> placeFromPaletteDrop(col, row)).register(root);
  }

  @Override
  public void saveLevel(String fileName) {
    commitSplineDraftBeforeSave();
    persistence.save(fileName);
  }

  private void commitSplineDraftBeforeSave() {
    TerrainSpline committed = splineDraft.commit();
    if (committed != null) {
      history.record(CanvasAction.place(committed));
      canvas.refreshGrid();
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
    spawnPointService.getSpawnPoint();
    canvas.refreshGrid();
  }

  GameObject placeSelectedObject(int col, int row) {
    if (selectedPlaceable == null) {
      return null;
    }
    double cellSize = canvas.getCellSize();
    AuthoringPlaceable placedObject = selectedPlaceable.createAt(col, row, cellSize);
    world.addObject((GameObject) placedObject);
    history.record(CanvasAction.place(placedObject));
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
        action.friction(), action.color(), action.restitution());
    world.addObject(block);
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
