package oogasalad.view.authoring.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests default geometry helpers for authoring placeables.
 *
 * @author Emilio Garcia
 */
class AuthoringPlaceableTest {

  @Test
  void ColSpan_WidthLargerThanCellSize_RoundsUpToCoveredColumns() {
    AuthoringPlaceable placeable = new StubPlaceable(10.0, 20.0, 125.0, 61.0);

    assertEquals(3, placeable.colSpan(60.0));
  }

  @Test
  void Bottom_TopAndHeightProvided_ReturnsBottomEdge() {
    AuthoringPlaceable placeable = new StubPlaceable(10.0, 20.0, 125.0, 61.0);

    assertEquals(81.0, placeable.bottom());
  }

  @Test
  void CreateAt_DefaultImplementation_ThrowsUnsupportedOperationException() {
    AuthoringPlaceable placeable = new StubPlaceable(10.0, 20.0, 125.0, 61.0);

    assertThrows(UnsupportedOperationException.class, () -> placeable.createAt(1, 1, 60.0));
  }

  private record StubPlaceable(double x, double y, double width, double height)
      implements AuthoringPlaceable {

    @Override
    public double rotation() {
      return 0.0;
    }

    @Override
    public double getWidth() {
      return width;
    }

    @Override
    public double getHeight() {
      return height;
    }

    @Override
    public String getColor() {
      return "#FFFFFF";
    }

    @Override
    public String getSprite() {
      return "";
    }

    @Override
    public boolean isTiled() {
      return false;
    }

    @Override
    public double getFriction() {
      return 0.5;
    }

    @Override
    public double getRestitution() {
      return 0.0;
    }
  }
}
