package oogasalad.controller.authoring.internal;

import java.util.Optional;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Locates and moves the finish-line marker in the authoring world.
 * The GoalLine is stored as an {@link AuthoringStaticComponent} with typeId "GoalLine".
 *
 * @author Michael Li
 */
public class GoalLineService {

  private static final String GOAL_LINE_TYPE = "GoalLine";
  private static final String GOAL_LINE_COLOR = "#FFD700";
  private static final double DEFAULT_COL = 50.0;
  private static final double DEFAULT_ROW = 0.0;
  private static final double DEFAULT_WIDTH = 1.0;
  private static final double DEFAULT_HEIGHT = 1.0;

  private final World world;

  public GoalLineService(World world) {
    this.world = world;
  }

  public Optional<AuthoringStaticComponent> getGoalLine() {
    for (GameComponent c : world.getAllComponents()) {
      if (c instanceof AuthoringStaticComponent a && GOAL_LINE_TYPE.equals(a.getComponentType())) {
        return Optional.of(a);
      }
    }
    return Optional.empty();
  }

  public void moveGoalLineTo(double col) {
    getGoalLine().ifPresent(gl -> {
      world.removeObject(gl);
      world.addObject(new AuthoringStaticComponent(
          col, gl.y(), gl.getWidth(), gl.getHeight(),
          GOAL_LINE_TYPE, gl.getColor(), gl.getSprite()));
    });
  }
}
