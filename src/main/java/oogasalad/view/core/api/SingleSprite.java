package oogasalad.view.core.api;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Raster sprite backed by one image path, scaled to the record width and height.
 *
 * @param path   image path resolved through SpriteCache
 * @param width  draw width
 * @param height draw height
 *
 * @author Paulina Vvedenskaya
 * @author Iurii Beliaev
 */
public record SingleSprite(String path, double width, double height) implements Sprite {

  /** Ensures non-null asset path prior to constructing the sprite. */
  public SingleSprite {
    Objects.requireNonNull(path, "path");
  }

  /** Loads from cache when present; otherwise draws a fallback rectangle preview. */
  @Override
  public Node renderPreview(SpriteCache cache) {
    Image image = cache.get(path);
    if (image == null) {
      Rectangle fallback = new Rectangle(width, height);
      fallback.setFill(Color.DODGERBLUE);
      fallback.setStroke(Color.BLACK);
      fallback.setStrokeWidth(1.0);
      return fallback;
    }
    ImageView view = new ImageView(image);
    view.setFitWidth(width);
    view.setFitHeight(height);
    return view;
  }
}
