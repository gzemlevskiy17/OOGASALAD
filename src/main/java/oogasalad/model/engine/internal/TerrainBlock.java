package oogasalad.model.engine.internal;

import oogasalad.model.engine.api.Pose;

/**
 * Concrete static terrain object (ground, ramp, wall) created by LevelLoaderImpl
 * and placed by the authoring canvas.
 *
 * Static by design: mass = 0, not affected by gravity, never lethal by default.
 * Implements getSprite for authoring/data serialization and relies on
 * GameComponent for Renderable position delegation.
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
    super(new Pose(x, y, 0), 0, friction);
    setAffectedByGravity(false);
    this.width = width;
    this.height = height;
    this.sprite = sprite;
    this.tiled = tiled;
  }

  @Override
  public String getSprite() {
    return sprite;
  }

  @Override
  public double getWidth() {
    return width;
  }

  @Override
  public double getHeight() {
    return height;
  }

  @Override
  public boolean isTiled() {
    return tiled;
  }
}
