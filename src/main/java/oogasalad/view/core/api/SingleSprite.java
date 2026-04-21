package oogasalad.view.core.api;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import oogasalad.view.core.internal.SpriteCache;

/**
 * A sprite drawn as a single image. Used for every component type that isn't a wheel.
 *
 * @author Paulina Vvedenskaya
 */
public record SingleSprite(String path, double width, double height) implements Sprite {

  public SingleSprite {
    Objects.requireNonNull(path, "path");
  }

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
