package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

/**
 * Concrete static terrain object (ground, ramp, wall) created by LevelLoaderImpl
 * and placed by the authoring canvas.
 *
 * Static by design: mass = 0, not affected by gravity, never lethal by default.
 * Implements getX/getY/getRotation/getSprite from Renderable since GameComponent
 * does not yet provide those delegations.
 *
 * Collaborators: LevelLoaderImpl (creates instances from JSON),
 *                LevelSaverImpl (serializes back to JSON),
 *                AuthoringCanvasImpl (renders onto editor grid),
 *                AuthoringControllerImpl (places on grid interaction).
 */
public class TerrainBlock extends TerrainComponent {

  private final double width;
  private final double height;
  private final String sprite;
  private final boolean tiled;

  /**
   * @param x        grid x-coordinate (left edge)
   * @param y        grid y-coordinate (bottom edge)
   * @param width    width in grid units
   * @param height   height in grid units
   * @param sprite   texture path or identifier
   * @param tiled    true → repeat texture; false → single scaled sprite
   * @param friction surface friction coefficient (non-negative)
   */
  public TerrainBlock(double x, double y, double width, double height,
      String sprite, boolean tiled, double friction) {
    super(new Pose(x, y, 0), 0, friction, false);
    setAffectedByGravity(false);
    this.width = width;
    this.height = height;
    this.sprite = sprite;
    this.tiled = tiled;
  }

  @Override
  public double getX() {
    return getPose().x();
  }

  @Override
  public double getY() {
    return getPose().y();
  }

  @Override
  public double getRotation() {
    return getPose().rotation();
  }

  @Override
  public String getSprite() {
    return sprite;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  public boolean isTiled() {
    return tiled;
  }
}
