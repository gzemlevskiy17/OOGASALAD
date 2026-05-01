package oogasalad.view.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import oogasalad.view.core.internal.SpriteCache;
import org.junit.jupiter.api.Test;

class SingleSpriteTest {

  @Test
  void renderPreview_missingSprite_returnsNeutralPlaceholder() {
    SingleSprite sprite = new SingleSprite("does/not/exist.png", 40, 30);

    Node node = sprite.renderPreview(new SpriteCache());

    StackPane placeholder = assertInstanceOf(StackPane.class, node);
    Rectangle background = assertInstanceOf(Rectangle.class, placeholder.getChildren().get(0));
    Text label = assertInstanceOf(Text.class, placeholder.getChildren().get(1));
    assertEquals(40, background.getWidth(), 1e-9);
    assertEquals(30, background.getHeight(), 1e-9);
    assertEquals("?", label.getText());
  }
}
