package oogasalad.view.core.api;

import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
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

  private static final Color MISSING_SPRITE_FILL = Color.rgb(245, 245, 245, 0.65);
  private static final Color MISSING_SPRITE_STROKE = Color.web("#B8B8B8");
  private static final String MISSING_SPRITE_LABEL = "?";

  /** Ensures non-null asset path prior to constructing the sprite. */
  public SingleSprite {
    Objects.requireNonNull(path, "path");
  }

  /** Loads from cache when present; otherwise draws a fallback rectangle preview. */
  @Override
  public Node renderPreview(SpriteCache cache) {
    Image image = cache.get(path);
    if (image == null) {
      return missingSpriteNode();
    }
    ImageView view = new ImageView(image);
    view.setFitWidth(width);
    view.setFitHeight(height);
    return view;
  }

  private Node missingSpriteNode() {
    Rectangle background = new Rectangle(width, height);
    background.setFill(MISSING_SPRITE_FILL);
    background.setStroke(MISSING_SPRITE_STROKE);
    background.getStrokeDashArray().setAll(4.0, 4.0);
    background.setStrokeWidth(1.0);

    Text label = new Text(MISSING_SPRITE_LABEL);
    label.setFill(Color.web("#666666"));
    StackPane fallback = new StackPane(background, label);
    fallback.setAlignment(Pos.CENTER);
    fallback.setMinSize(width, height);
    fallback.setPrefSize(width, height);
    fallback.setMaxSize(width, height);
    return fallback;
  }
}
