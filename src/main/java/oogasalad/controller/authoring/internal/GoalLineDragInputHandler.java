package oogasalad.controller.authoring.internal;

import java.util.Objects;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Lets the user drag the finish-line stripe horizontally across the canvas.
 * Activates when a press lands within {@link #HIT_RADIUS_PX} pixels of the stripe's x edge.
 * Only the x coordinate is updated — y stays fixed so the stripe always spans full height.
 *
 * @author Michael Li
 */
public class GoalLineDragInputHandler implements CanvasInputHandler {

  private static final double HIT_RADIUS_PX = 20.0;

  private final AuthoringCanvas canvas;
  private final GoalLineService goalLineService;
  private boolean active;

  public GoalLineDragInputHandler(AuthoringCanvas canvas, GoalLineService goalLineService) {
    this.canvas = Objects.requireNonNull(canvas);
    this.goalLineService = Objects.requireNonNull(goalLineService);
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public void register(Node root) {
    root.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
    root.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
    root.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
  }

  private void handleMousePressed(MouseEvent event) {
    if (isNearGoalLine(event.getX())) {
      active = true;
      event.consume();
    }
  }

  private void handleMouseDragged(MouseEvent event) {
    if (!active) {
      return;
    }
    moveGoalLineTo(event.getX());
    event.consume();
  }

  private void handleMouseReleased(MouseEvent event) {
    if (!active) {
      return;
    }
    moveGoalLineTo(event.getX());
    active = false;
    event.consume();
  }

  private void moveGoalLineTo(double pixelX) {
    double col = pixelX / canvas.getCellSize();
    goalLineService.moveGoalLineTo(col);
    canvas.refreshGrid();
  }

  private boolean isNearGoalLine(double pixelX) {
    Optional<AuthoringStaticComponent> gl = goalLineService.getGoalLine();
    if (gl.isEmpty()) {
      return false;
    }
    double glPixelX = gl.get().x() * canvas.getCellSize();
    return Math.abs(pixelX - glPixelX) <= HIT_RADIUS_PX;
  }
}
