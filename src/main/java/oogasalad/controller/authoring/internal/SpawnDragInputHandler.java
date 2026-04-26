package oogasalad.controller.authoring.internal;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import oogasalad.controller.authoring.api.ToolSelector;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Lets the user drag the spawn-point marker around the canvas. Activates only when a press
 * starts near the marker and the spline tool is not active. Other handlers can check
 * {@link #isActive()} to suppress their own gestures during a spawn drag.
 *
 * @author Paulina Vvedenskaya
 */
public class SpawnDragInputHandler implements CanvasInputHandler {

  private static final double HIT_RADIUS_CELLS = 1.5;

  private final AuthoringCanvas canvas;
  private final SpawnPointService spawnPointService;
  private final ToolSelector toolSelector;
  private boolean active;

  /**
   * @param canvas            canvas providing cell size and refresh hooks
   * @param spawnPointService source/target for the spawn-point lookup and move
   * @param toolSelector      source of truth for the currently-active tool
   */
  public SpawnDragInputHandler(AuthoringCanvas canvas, SpawnPointService spawnPointService,
      ToolSelector toolSelector) {
    this.canvas = Objects.requireNonNull(canvas);
    this.spawnPointService = Objects.requireNonNull(spawnPointService);
    this.toolSelector = Objects.requireNonNull(toolSelector);
  }

  /**
   * @return {@code true} between the press that landed on the spawn marker and the matching
   * release; {@code false} otherwise. Other handlers consult this to decide whether to suppress
   * their own gestures.
   */
  public boolean isActive() {
    return active;
  }

  @Override
  public void register(Node root) {
    Objects.requireNonNull(root);
    root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handleMousePressed(event));
    root.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> handleMouseDragged(event));
    root.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> handleMouseReleased(event));
  }

  private void handleMousePressed(MouseEvent event) {
    if (isSplineMode()) {
      return;
    }
    if (isNearSpawn(event.getX(), event.getY())) {
      active = true;
    }
  }

  private void handleMouseDragged(MouseEvent event) {
    if (!active) {
      return;
    }
    moveSpawnTo(event.getX(), event.getY());
  }

  private void handleMouseReleased(MouseEvent event) {
    if (!active) {
      return;
    }
    moveSpawnTo(event.getX(), event.getY());
    active = false;
  }

  private boolean isSplineMode() {
    return toolSelector.getActiveTool() == ToolType.SPLINE;
  }

  private void moveSpawnTo(double pixelX, double pixelY) {
    double cellSize = canvas.getCellSize();
    spawnPointService.moveSpawnTo(pixelX / cellSize, pixelY / cellSize);
    canvas.refreshGrid();
  }

  private boolean isNearSpawn(double pixelX, double pixelY) {
    SpawnPointComponent spawn = spawnPointService.getSpawnPoint();
    double cellSize = canvas.getCellSize();
    double dx = pixelX - spawn.x() * cellSize;
    double dy = pixelY - spawn.y() * cellSize;
    double hitRadius = cellSize * HIT_RADIUS_CELLS;
    return dx * dx + dy * dy <= hitRadius * hitRadius;
  }
}
