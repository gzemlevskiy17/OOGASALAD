package oogasalad.view.core.api;

import java.util.Objects;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import oogasalad.view.core.internal.SpriteCache;

/**
 * Tire layer below hub layer: cosmetic spin proportional to rolling speed divided by nominal radius while
 * the outer wrapper ignores body yaw.
 *
 * @param hubPath   texture for the stationary center piece
 * @param wheelPath texture for the rotating tire layer drawn first in the Group
 * @param width     layout width hints
 * @param height    layout height hints
 * @author Paulina Vvedenskaya
 */
public record LayeredSprite(String hubPath, String wheelPath, double width, double height)
    implements Sprite {

  private static final double FRAME_SECONDS = 1.0 / 60.0;
  private static final double STATIONARY_SPEED_THRESHOLD = 1.0;

  /** Validates non-null hub and wheel paths. */
  public LayeredSprite {
    Objects.requireNonNull(hubPath, "hubPath");
    Objects.requireNonNull(wheelPath, "wheelPath");
  }

  /** Returns a Group with wheel first, hub second, for update targeting the first child. */
  @Override
  public Node renderPreview(SpriteCache cache) {
    return new Group(imageLayer(cache.get(wheelPath)), imageLayer(cache.get(hubPath)));
  }

  /** Vehicle frames rotate separate pieces; outer node does not mirror Renderable rotation. */
  @Override
  public boolean usesRenderableRotation() {
    return false;
  }

  /** Increments wheel child rotation from signed rolling speed when above the stationary threshold. */
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
