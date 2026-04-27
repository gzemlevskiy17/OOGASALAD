package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;

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

  private static final String DEFAULT_COLOR = "#8B4513";

  private final double width;
  private final double height;
  private final String sprite;
  private final boolean tiled;
  private final String color;
  private final String catalogName;

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
      String sprite, boolean tiled, double friction, double restitution) {

    this(x, y, width, height, sprite, tiled, friction, DEFAULT_COLOR, restitution, null);
    setPolygon(ConvexPolygon.axisAlignedBox(x, y, width, height));
  }

  /**
   * @param color CSS color string used as a fallback when the sprite cannot be loaded
   */
  public TerrainBlock(double x, double y, double width, double height,
      String sprite, boolean tiled, double friction, String color, double restitution) {
    this(x, y, width, height, sprite, tiled, friction, color, restitution, null);
  }

  /**
   * @param catalogName key from the authoring terrain catalog, e.g. {@code "Earth"}; may be
   *                    {@code null} for levels loaded from JSON
   */
  public TerrainBlock(double x, double y, double width, double height,
      String sprite, boolean tiled, double friction, String color, double restitution,
      String catalogName) {
    super(
        x,
        y,
        0,
        ConvexPolygon.axisAlignedBox(x, y, width, height),
        sprite,
        friction,
        restitution);

    this.width = width;
    this.height = height;
    this.sprite = sprite;
    this.tiled = tiled;
    this.color = color;
    this.catalogName = catalogName;
  }

  @Override
  public String getCatalogName() {
    return catalogName;
  }

  /** @return the CSS color string used as a fallback when the sprite cannot be loaded */
  public String getColor() {
    return color;
  }

  @Override
  public String getSprite() {
    return sprite;
  }

  @Override
  public double getWidth() {
    // TODO: implement polygon width
    return width;
  }

  @Override
  public double getHeight() {
    // TODO: implement polygon height
    return height;
  }

  @Override
  public boolean isTiled() {
    return tiled;
  }
}
