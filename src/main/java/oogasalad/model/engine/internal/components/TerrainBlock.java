package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.GameObject;

/**
 * Concrete static terrain object (ground, ramp, wall) created by LevelLoaderImpl and placed by the
 * authoring canvas.
 * <p>
 * Static by design: mass = 0, not affected by gravity, never lethal by default. Implements
 * getSprite for authoring/data serialization and relies on GameComponent for Renderable position
 * delegation.
 * <p>
 * Collaborators: LevelLoaderImpl (creates instances from JSON), LevelSaverImpl (serializes back to
 * JSON), AuthoringCanvasImpl (renders onto editor grid), AuthoringControllerImpl (places on grid
 * interaction).
 */
public class TerrainBlock extends TerrainComponent {

  private final boolean tiled;

  /**
   * @param x        grid x-coordinate (left edge)
   * @param y        grid y-coordinate (bottom edge)
   * @param tiled    true → repeat texture; false → single scaled sprite
   * @param friction surface friction coefficient (non-negative)
   */
  public TerrainBlock(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      boolean tiled, double friction) {
    super(x, y, 0, polygon, tag, friction);
    this.tiled = tiled;
  }

  @Override
  public double getWidth() {
    //TODO: implement polygon width
    return 0;
  }

  @Override
  public double getHeight() {
    //TODO: implement polygon height
    return 0;
  }

  @Override
  public boolean isTiled() {
    return tiled;
  }

  /**
   * @param other
   */
  @Override
  public void collideWith(GameObject other) {

  }

  /**
   * @param component
   */
  @Override
  public void handleCollision(StaticComponent component) {

  }

  /**
   * @param component
   */
  @Override
  public void handleCollision(DynamicComponent component) {

  }
}
