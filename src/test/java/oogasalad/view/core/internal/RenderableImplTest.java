package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderableImplTest {

  @Test
  void constructor_storesPositiveValues() {
    RenderableImpl r = new RenderableImpl(10.5, 20.75, 45.0);
    assertEquals(10.5, r.x(), 1e-9);
    assertEquals(20.75, r.y(), 1e-9);
    assertEquals(45.0, r.rotation(), 1e-9);
  }

  @Test
  void constructor_storesNegativeValues() {
    RenderableImpl r = new RenderableImpl(-5.0, -100.0, -90.0);
    assertEquals(-5.0, r.x(), 1e-9);
    assertEquals(-100.0, r.y(), 1e-9);
    assertEquals(-90.0, r.rotation(), 1e-9);
  }

  @Test
  void constructor_storesZeros() {
    RenderableImpl r = new RenderableImpl(0.0, 0.0, 0.0);
    assertEquals(0.0, r.x(), 1e-9);
    assertEquals(0.0, r.y(), 1e-9);
    assertEquals(0.0, r.rotation(), 1e-9);
  }

  @Test
  void getX_isIndependentOfYAndRotation() {
    RenderableImpl r1 = new RenderableImpl(3.0, 100.0, 200.0);
    RenderableImpl r2 = new RenderableImpl(3.0, 999.0, 999.0);
    assertEquals(r1.x(), r2.x(), 1e-9);
  }

  @Test
  void getY_isIndependentOfXAndRotation() {
    RenderableImpl r1 = new RenderableImpl(100.0, 7.5, 200.0);
    RenderableImpl r2 = new RenderableImpl(999.0, 7.5, 999.0);
    assertEquals(r1.y(), r2.y(), 1e-9);
  }

  @Test
  void rotationOver360_storedExactly() {
    // The class should not normalise angles — store what was given
    RenderableImpl r = new RenderableImpl(0, 0, 720.0);
    assertEquals(720.0, r.rotation(), 1e-9);
  }
}
