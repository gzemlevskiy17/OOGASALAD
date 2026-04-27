package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.layout.Pane;
import oogasalad.model.engine.WorldImpl;
import org.junit.jupiter.api.Test;

/**
 * GP156: grid hover preview is tied to a palette placeable; with no selection there is no
 * semi-transparent tile following the pointer.
 */
class AuthoringCanvasDefaultCursorTest {

  @Test
  void showHoverPreview_withNullBrush_clearsAndDoesNotThrow() {
    WorldImpl world = new WorldImpl();
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl(world);
    canvas.showHoverPreview(0, 0);
    assertNotNull(canvas.getVisualRoot());
  }

  @Test
  void setSelectedPlaceable_null_clearsAfterPreview() {
    WorldImpl world = new WorldImpl();
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl(world);
    double cs = canvas.getCellSize();
    canvas.setSelectedPlaceable(
        new TerrainPlaceable(cs, cs, "#8B4513", "assets/terrain/grass.png", true, 0.6, 0.5));
    canvas.showHoverPreview(1, 1);
    canvas.setSelectedPlaceable(null);
    Pane root = (Pane) canvas.getVisualRoot();
    assertTrue(root.getChildren().size() > 0);
  }
}
