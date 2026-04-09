package oogasalad.view.core.internal;

import oogasalad.view.core.api.Renderable;

public class RenderableImpl implements Renderable {

  private final double x;
  private final double y;
  private final double rotation;

  public RenderableImpl(double x, double y, double rotation) {
    this.x = x;
    this.y = y;
    this.rotation = rotation;
  }

  @Override
  public double getX() {
    return x;
  }

  @Override
  public double getY() {
    return y;
  }

  @Override
  public double getRotation() {
    return rotation;
  }

}
