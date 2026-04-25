package oogasalad.view.core.api;

import java.util.Objects;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import oogasalad.view.core.internal.SpriteCache;

/**
 * A two-layer sprite: a tire/wheel image that spins plus a hub image that stays rigid. The tire's
 * spin is cosmetic and derived from signed rolling speed via {@code angularVelocity = speed / r}.
 *
 * @author Paulina Vvedenskaya
 */
public record LayeredSprite(String hubPath, String wheelPath, double width, double height)
    implements Sprite {

  private static final double FRAME_SECONDS = 1.0 / 60.0;
  private static final double STATIONARY_SPEED_THRESHOLD = 1.0;

  public LayeredSprite {
    Objects.requireNonNull(hubPath, "hubPath");
    Objects.requireNonNull(wheelPath, "wheelPath");
  }

  @Override
  public Node renderPreview(SpriteCache cache) {
    return new Group(imageLayer(cache.get(wheelPath)), imageLayer(cache.get(hubPath)));
  }

  @Override
  public boolean usesRenderableRotation() {
    return false;
  }

  @Override
  public void update(Node node, double rollingSpeed) {
    if (Math.abs(rollingSpeed) < STATIONARY_SPEED_THRESHOLD) {
      return;
    }
    if (!(node instanceof Group group) || group.getChildren().isEmpty()) {
      return;
    }
    Node tire = group.getChildren().get(0);
    double radius = Math.max(width, height) / 2.0;
    double angularVelocity = rollingSpeed / Math.max(radius, 1.0);
    double deltaDeg = Math.toDegrees(angularVelocity) * FRAME_SECONDS;
    tire.setRotate(tire.getRotate() + deltaDeg);
  }

  private ImageView imageLayer(Image image) {
    ImageView view = new ImageView(image);
    view.setFitWidth(width);
    view.setFitHeight(height);
    return view;
  }
}
