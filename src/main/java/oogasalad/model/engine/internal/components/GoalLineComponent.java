package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.view.core.api.SpriteRenderable;

/**
 * A visible finish-line column placed in the world. The pig wins when it reaches this zone.
 *
 * Non-physical (trigger), not affected by gravity, tagged as "finish-line".
 *
 * @author Paulina Vvedenskaya
 */
public class GoalLineComponent extends FinishLineComponent implements SpriteRenderable {

  private final double width;
  private final double height;

  public GoalLineComponent(double x, double y, double width, double height) {
    super(x, y, 0, ConvexPolygon.axisAlignedBox(x, y, width, height), ""); // fix later for extendability
    this.width = width;
    this.height = height;
  }

  // TODO: fix Constructor anchors the box for polygon at x, y, and this fixes it to center at (x, y)
  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(
        getX() - width / 2, getY() - height / 2, width, height));
  }

  public void applyEffect(GameObject target) {
    // win detection is handled by FinishLineWinChecker, not by the component itself
  }

  @Override
  public String getComponentType() {
    return "GoalLine";
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  @Override
  public void addForce(Vector force) {
    // Non-physical zone: ignore forces.
  }

  @Override
  public void setRestitution(double restitution) {
    // No rigid-body restitution for this zone unless you store it later.
  }

  @Override
  public double getFriction() {
    return 0.0;
  }

  @Override
  public boolean isAffectedByGravity() {
    return false;
  }

  @Override
  public boolean isTrigger() {
    return true;
  }
}
