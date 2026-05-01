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
 * A sprite drawn as a single image. Used for every component type that isn't a wheel.
 *
 * @author Paulina Vvedenskaya
 */
public record SingleSprite(String path, double width, double height) implements Sprite {

  private static final Color MISSING_SPRITE_FILL = Color.rgb(245, 245, 245, 0.65);
  private static final Color MISSING_SPRITE_STROKE = Color.web("#B8B8B8");
  private static final String MISSING_SPRITE_LABEL = "?";

  public SingleSprite {
    Objects.requireNonNull(path, "path");
  }

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
