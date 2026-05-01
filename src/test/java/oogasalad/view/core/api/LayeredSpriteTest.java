package oogasalad.view.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javafx.scene.Group;
import javafx.scene.Node;
import oogasalad.view.core.internal.SpriteCache;
import org.junit.jupiter.api.Test;

/** Unit tests for LayeredSprite rendering flags and per-frame wheel spin behavior. */
class LayeredSpriteTest {

  @Test
  void render_marksLayeredWheelAsNotUsingBodyRotation() {
    LayeredSprite sprite = new LayeredSprite("missing-hub.png", "missing-wheel.png", 60, 60);

    Node node = sprite.render(new SpriteCache());

    assertFalse((Boolean) node.getProperties().get(Sprite.USES_RENDERABLE_ROTATION_KEY));
  }

  @Test
  void update_spinsOnlyWheelLayerAndLeavesHubConnectorFixed() {
    LayeredSprite sprite = new LayeredSprite("missing-hub.png", "missing-wheel.png", 60, 60);
    Group node = (Group) sprite.render(new SpriteCache());

    sprite.update(node, 120.0);

    assertEquals(0.0, node.getRotate(), 1e-9);
    assertEquals(0.0, node.getChildren().get(1).getRotate(), 1e-9);
    assertEquals(Math.toDegrees((120.0 / 30.0) * (1.0 / 60.0)),
        node.getChildren().get(0).getRotate(), 1e-9);
  }

  @Test
  void update_negativeRollingSpeedSpinsWheelBackward() {
    LayeredSprite sprite = new LayeredSprite("missing-hub.png", "missing-wheel.png", 60, 60);
    Group node = (Group) sprite.render(new SpriteCache());

    sprite.update(node, -120.0);

    assertEquals(Math.toDegrees((-120.0 / 30.0) * (1.0 / 60.0)),
        node.getChildren().get(0).getRotate(), 1e-9);
  }

  @Test
  void update_zeroRollingSpeedDoesNotSpinWheel() {
    LayeredSprite sprite = new LayeredSprite("missing-hub.png", "missing-wheel.png", 60, 60);
    Group node = (Group) sprite.render(new SpriteCache());

    sprite.update(node, 0.0);

    assertEquals(0.0, node.getChildren().get(0).getRotate(), 1e-9);
  }
}
