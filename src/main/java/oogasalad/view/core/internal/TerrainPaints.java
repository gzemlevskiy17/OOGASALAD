package oogasalad.view.core.internal;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Shape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Paint helpers for terrain shapes that may use a sprite texture or a solid-color fallback.
 *
 * @author Paulina Vvedenskaya
 */
public final class TerrainPaints {

  private static final Logger LOG = LogManager.getLogger(TerrainPaints.class);
  private static final Color DEFAULT_FALLBACK = Color.SADDLEBROWN;

  private TerrainPaints() {
  }

  /**
   * Applies a sprite texture to the shape when possible, otherwise applies the fallback color.
   *
   * @param shape      shape to paint
   * @param spritePath filesystem or classpath path to an image
   * @param fallbackCss CSS color used when no sprite can be loaded
   * @param width      paint width in local units
   * @param height     paint height in local units
   * @param tiled      whether to tile the sprite instead of stretching it
   */
  public static void applyTerrainFill(Shape shape, String spritePath, String fallbackCss,
      double width, double height, boolean tiled) {
    Image image = loadImage(spritePath);
    if (image != null) {
      double paintWidth = Math.max(1.0, tiled ? image.getWidth() : width);
      double paintHeight = Math.max(1.0, tiled ? image.getHeight() : height);
      shape.setFill(new ImagePattern(image, 0, 0, paintWidth, paintHeight, false));
      return;
    }
    shape.setFill(parseColor(fallbackCss));
  }

  /**
   * Parses a CSS color string with a safe terrain fallback.
   *
   * @param css CSS color string
   * @return parsed color or the default terrain color
   */
  public static Color parseColor(String css) {
    if (css == null || css.isBlank()) {
      return DEFAULT_FALLBACK;
    }
    try {
      return Color.web(css);
    } catch (IllegalArgumentException e) {
      return DEFAULT_FALLBACK;
    }
  }

  private static Image loadImage(String spritePath) {
    if (spritePath == null || spritePath.isBlank()) {
      return null;
    }
    try {
      java.io.File file = new java.io.File(spritePath);
      if (file.isFile()) {
        return new Image(file.toURI().toString());
      }
      String normalized = spritePath.startsWith("/") ? spritePath.substring(1) : spritePath;
      java.net.URL resource = TerrainPaints.class.getClassLoader().getResource(normalized);
      if (resource != null) {
        return new Image(resource.toExternalForm());
      }
    } catch (Exception e) {
      LOG.warn("Could not load terrain sprite: {}", spritePath);
    }
    return null;
  }
}
