package oogasalad.view.authoring.internal;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import oogasalad.view.authoring.api.AuthoringCanvas;

/**
 * Wires zoom and edge-panning behavior for the authoring canvas viewport.
 *
 * @author Cody
 */
final class CanvasZoomPanHandler {

  private static final double MIN_SCROLL_VALUE = 0.0;
  private static final double MAX_SCROLL_VALUE = 1.0;

  static final class Settings {

    private final double edgePanThreshold;
    private final double edgePanSpeed;
    private final double zoomStep;

    Settings(double edgePanThreshold, double edgePanSpeed, double zoomStep) {
      this.edgePanThreshold = edgePanThreshold;
      this.edgePanSpeed = edgePanSpeed;
      this.zoomStep = zoomStep;
    }
  }

  private final AuthoringCanvas canvas;
  private final ScrollPane scrollPane;
  private final Settings settings;
  private double velocityX;
  private double velocityY;

  CanvasZoomPanHandler(AuthoringCanvas canvas, ScrollPane scrollPane, Settings settings) {
    this.canvas = canvas;
    this.scrollPane = scrollPane;
    this.settings = settings;
  }

  void wireHandlers() {
    wireEdgePan();
    wireZoomScroll();
  }

  private void wireEdgePan() {
    EventHandler<MouseEvent> edgeDetect = event -> updateEdgeVelocity(event);
    scrollPane.addEventFilter(MouseEvent.MOUSE_MOVED, edgeDetect);
    scrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, edgeDetect);
    scrollPane.addEventFilter(MouseEvent.MOUSE_EXITED, event -> stopEdgePan());
    new AnimationTimer() {
      @Override
      public void handle(long now) {
        applyEdgePan();
      }
    }.start();
  }

  private void updateEdgeVelocity(MouseEvent event) {
    double viewportWidth = scrollPane.getViewportBounds().getWidth();
    double viewportHeight = scrollPane.getViewportBounds().getHeight();
    velocityX = edgeVelocity(event.getX(), viewportWidth, settings.edgePanThreshold,
        settings.edgePanSpeed);
    velocityY = edgeVelocity(event.getY(), viewportHeight, settings.edgePanThreshold,
        settings.edgePanSpeed);
  }

  private void stopEdgePan() {
    velocityX = MIN_SCROLL_VALUE;
    velocityY = MIN_SCROLL_VALUE;
  }

  private void applyEdgePan() {
    if (velocityX != MIN_SCROLL_VALUE) {
      scrollPane.setHvalue(clampScrollValue(scrollPane.getHvalue() + velocityX));
    }
    if (velocityY != MIN_SCROLL_VALUE) {
      scrollPane.setVvalue(clampScrollValue(scrollPane.getVvalue() + velocityY));
    }
  }

  private void wireZoomScroll() {
    scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
      if (!event.isControlDown()) {
        return;
      }
      canvas.setZoom(zoomedFactor(event));
      event.consume();
    });
  }

  private double zoomedFactor(ScrollEvent event) {
    if (event.getDeltaY() > MIN_SCROLL_VALUE) {
      return canvas.getZoom() * settings.zoomStep;
    }
    return canvas.getZoom() / settings.zoomStep;
  }

  private static double edgeVelocity(double position, double size, double threshold, double speed) {
    if (position < threshold && threshold > MIN_SCROLL_VALUE) {
      return -speed * (MAX_SCROLL_VALUE - position / threshold);
    }
    if (position > size - threshold && threshold > MIN_SCROLL_VALUE) {
      return speed * (MAX_SCROLL_VALUE - (size - position) / threshold);
    }
    return MIN_SCROLL_VALUE;
  }

  private static double clampScrollValue(double value) {
    return Math.max(MIN_SCROLL_VALUE, Math.min(MAX_SCROLL_VALUE, value));
  }
}
