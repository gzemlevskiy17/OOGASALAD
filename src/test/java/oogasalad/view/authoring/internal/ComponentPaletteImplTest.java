package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;

class ComponentPaletteImplTest {

  @Test
  void setDragEnabled_falseKeepsTerrainPaletteSelectionButDisablesDragPlacement() {
    ComponentPaletteImpl palette = new ComponentPaletteImpl();
    palette.setDragEnabled(false);

    palette.setItems(List.of(new ComponentPaletteImpl.PaletteEntry(
        "DirtPath", "#8B5E3C", 1, 1, "data/assets/back.png", true, 0.4, 0.2, false)));

    Canvas sample = firstSample(palette);
    assertNotNull(sample.getOnMouseClicked());
    assertNull(sample.getOnDragDetected());
  }

  @Test
  void setDragEnabled_trueAllowsLevelItemPaletteDragPlacement() {
    ComponentPaletteImpl palette = new ComponentPaletteImpl();

    palette.setItems(List.of(new ComponentPaletteImpl.PaletteEntry(
        "Coin", "#FFD700", 1, 1, "data/assets/bp_coin.png", false, 0, 0, true)));

    Canvas sample = firstSample(palette);
    assertNotNull(sample.getOnMouseClicked());
    assertNotNull(sample.getOnDragDetected());
  }

  private Canvas firstSample(ComponentPaletteImpl palette) {
    ScrollPane root = (ScrollPane) palette.getVisualRoot();
    HBox content = (HBox) root.getContent();
    return (Canvas) content.getChildren().get(0);
  }
}
