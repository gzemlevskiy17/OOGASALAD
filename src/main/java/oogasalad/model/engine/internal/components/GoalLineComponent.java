package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.GameObject;

/**
 * A visible finish-line column placed in the world. The pig wins when it reaches this zone.
 *
 * Non-physical (trigger), not affected by gravity, tagged as "finish-line".
 *
 * @author Paulina Vvedenskaya
 */
public class GoalLineComponent extends FinishLineComponent {

  private final double width;
  private final double height;

  public GoalLineComponent(double x, double y, double width, double height) {
    super(x, y, 0, 0, 0);
    this.width = width;
    this.height = height;
    updatePolygon();
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(getX(), getY(), width, height));
  }

  @Override
  public void applyEffect(GameObject target) {
    // win detection is handled by FinishLineWinChecker, not by the component itself
  }

  public double getSpriteWidth() {
    return width;
  }

  public double getSpriteHeight() {
    return height;
  }
}
