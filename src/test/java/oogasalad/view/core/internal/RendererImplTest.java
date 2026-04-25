package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Sprite;
import org.junit.jupiter.api.Test;

class RendererImplTest {

  @Test
  void render_convertsRenderableRadiansToJavaFxDegrees() {
    Pane layer = new Pane();
    RendererImpl renderer = new RendererImpl(layer, renderable -> new Rectangle(10, 10),
        (renderable, node) -> {
        });
    Renderable object = new RenderableImpl(25, 50, Math.PI / 2.0);

    renderer.render(new FixedCamera(), List.of(object));

    assertEquals(90.0, layer.getChildren().getFirst().getRotate(), 1e-9);
  }

  @Test
  void render_positionsNodeRelativeToCamera() {
    Pane layer = new Pane();
    RendererImpl renderer = new RendererImpl(layer, renderable -> new Rectangle(10, 10),
        (renderable, node) -> {
        });
    Renderable object = new RenderableImpl(25, 50, 0.0);

    renderer.render(new FixedCamera(10, 20), List.of(object));

    assertEquals(15.0, layer.getChildren().getFirst().getLayoutX(), 1e-9);
    assertEquals(30.0, layer.getChildren().getFirst().getLayoutY(), 1e-9);
  }

  @Test
  void render_respectsSpriteOptOutFromBodyRotation() {
    Pane layer = new Pane();
    RendererImpl renderer = new RendererImpl(layer, renderable -> {
      Rectangle node = new Rectangle(10, 10);
      node.getProperties().put(Sprite.USES_RENDERABLE_ROTATION_KEY, false);
      return node;
    }, (renderable, node) -> {
    });
    Renderable object = new RenderableImpl(25, 50, Math.PI / 2.0);

    renderer.render(new FixedCamera(), List.of(object));

    assertEquals(0.0, layer.getChildren().getFirst().getRotate(), 1e-9);
  }

  private static final class FixedCamera implements Camera {

    private double x;
    private double y;

    private FixedCamera() {
      this(0, 0);
    }

    private FixedCamera(double x, double y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean isVisible(Renderable object) {
      return true;
    }

    @Override
    public double getX() {
      return x;
    }

    @Override
    public double getY() {
      return y;
    }

    @Override
    public double getViewWidth() {
      return 1000;
    }

    @Override
    public double getViewHeight() {
      return 1000;
    }

    @Override
    public void setPosition(double x, double y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public void setViewBoundaries(double width, double height) {
    }

    @Override
    public void setBehavior(CameraBehavior behavior) {
    }

    @Override
    public void updateBehavior() {
    }
  }
}
