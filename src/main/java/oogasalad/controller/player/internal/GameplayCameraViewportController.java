package oogasalad.controller.player.internal;

import oogasalad.view.core.api.Camera;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Keeps the gameplay camera viewport synchronized with the render layer size and zoom input.
 *
 * @author Cody
 */
final class GameplayCameraViewportController {

  private static final double MIN_VIEWPORT_SIZE = 1.0;
  private static final double DEFAULT_VIEW_WIDTH = 1280.0;
  private static final double DEFAULT_VIEW_HEIGHT = 720.0;
  private static final double ZOOM_STEP = 1.1;
  private static final double MIN_ZOOM = 0.5;
  private static final double MAX_ZOOM = 3.0;

  double initialViewWidth(GamePlayAppScreen screen) {
    double width = screen.getGameLayer().getWidth();
    if (width <= 0) {
      return DEFAULT_VIEW_WIDTH;
    }
    return width;
  }

  double initialViewHeight(GamePlayAppScreen screen) {
    double height = screen.getGameLayer().getHeight();
    if (height <= 0) {
      return DEFAULT_VIEW_HEIGHT;
    }
    return height;
  }

  void wireResponsiveCamera(GamePlayAppScreen screen, Camera camera) {
    ViewportState state = new ViewportState(
        Math.max(DEFAULT_VIEW_WIDTH, screen.getGameLayer().getWidth()),
        Math.max(DEFAULT_VIEW_HEIGHT, screen.getGameLayer().getHeight()));
    wireResizeHandlers(screen, camera, state);
    wireZoomHandler(screen, camera, state);
    state.applyTo(camera);
  }

  private void wireResizeHandlers(GamePlayAppScreen screen, Camera camera, ViewportState state) {
    screen.getGameLayer().widthProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        state.baseWidth = newVal.doubleValue();
        state.applyTo(camera);
      }
    });
    screen.getGameLayer().heightProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() > 0) {
        state.baseHeight = newVal.doubleValue();
        state.applyTo(camera);
      }
    });
  }

  private void wireZoomHandler(GamePlayAppScreen screen, Camera camera, ViewportState state) {
    screen.getGameLayer().setOnScroll(event -> {
      if (!event.isControlDown()) {
        return;
      }
      state.zoom = nextZoom(state.zoom, event.getDeltaY());
      state.applyTo(camera);
      event.consume();
    });
  }

  private double nextZoom(double currentZoom, double scrollDeltaY) {
    double next;
    if (scrollDeltaY > 0) {
      next = currentZoom * ZOOM_STEP;
    } else {
      next = currentZoom / ZOOM_STEP;
    }
    return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, next));
  }

  private static class ViewportState {

    double baseWidth;
    double baseHeight;
    double zoom = 1.0;

    ViewportState(double baseWidth, double baseHeight) {
      this.baseWidth = baseWidth;
      this.baseHeight = baseHeight;
    }

    void applyTo(Camera camera) {
      double viewWidth = Math.max(MIN_VIEWPORT_SIZE, baseWidth / zoom);
      double viewHeight = Math.max(MIN_VIEWPORT_SIZE, baseHeight / zoom);
      camera.setViewBoundaries(viewWidth, viewHeight);
    }
  }
}
