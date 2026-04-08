package oogasalad.controller.authoring.internal;

import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.AuthoringController;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.view.authoring.api.AuthoringCanvas;
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
 *                MichaelCanvas (refreshed after each model change).
 */
public class AuthoringControllerImpl implements AuthoringController {

  private static final Logger LOG = LogManager.getLogger(AuthoringControllerImpl.class);

  /** Pixels per grid unit — must match MichaelCanvas.CELL_SIZE. */
  public static final double CELL_SIZE = 40.0;

  private static final String DEFAULT_TERRAIN_TEXTURE = "assets/terrain/grass.png";
  private static final double DEFAULT_TERRAIN_FRICTION = 0.6;

  private final World world;
  private final LevelLoader loader;
  private final LevelSaver saver;
  private final GameConfig config;
  private final ToolSelector toolSelector;
  private final AuthoringCanvas canvas;

  public AuthoringControllerImpl(World world, LevelLoader loader, LevelSaver saver,
      GameConfig config, ToolSelector toolSelector, AuthoringCanvas canvas) {
    this.world = world;
    this.loader = loader;
    this.saver = saver;
    this.config = config;
    this.toolSelector = toolSelector;
    this.canvas = canvas;
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
    double gridX = Math.floor(x / CELL_SIZE);
    double gridY = Math.floor(y / CELL_SIZE);

    if (toolSelector.getActiveTool() == ToolType.DRAW) {
      placeTerrain(gridX, gridY);
    } else if (toolSelector.getActiveTool() == ToolType.ERASE) {
      // TODO: requires World.removeObject() — flag to Xuye to add to World interface.
      LOG.warn("Erase tool not yet supported: World.removeObject() pending API addition.");
    }

    canvas.refreshGrid();
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

  private void placeTerrain(double gridX, double gridY) {
    boolean cellOccupied = world.getObjects().stream()
        .anyMatch(obj -> obj.getX() == gridX && obj.getY() == gridY);
    if (!cellOccupied) {
      world.addObject(new TerrainBlock(gridX, gridY, 1, 1,
          DEFAULT_TERRAIN_TEXTURE, true, DEFAULT_TERRAIN_FRICTION));
    }
  }
}
