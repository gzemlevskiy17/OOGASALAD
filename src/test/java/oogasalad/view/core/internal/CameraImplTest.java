package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CameraImplTest {

  private CameraImpl camera;

  private static Renderable makeRenderable(double x, double y) {
    return new RenderableImpl(x, y, 0.0);
  }

  // ── constructor ───────────────────────────────────────────────────────────

  @BeforeEach
  void setUp() {
    // Camera at (0, 0) with 100x100 viewport
    camera = new CameraImpl(0.0, 0.0, 100.0, 100.0);
  }

  @Test
  void constructor_storesInitialPosition() {
    CameraImpl c = new CameraImpl(10.0, 20.0, 50.0, 60.0);
    assertEquals(10.0, c.getX(), 1e-9);
    assertEquals(20.0, c.getY(), 1e-9);
  }

  @Test
  void constructor_negativeViewportSize_throws() {
    assertThrows(IllegalArgumentException.class, () -> new CameraImpl(0, 0, -1, 100));
    assertThrows(IllegalArgumentException.class, () -> new CameraImpl(0, 0, 100, -1));
  }

  // ── setPosition ───────────────────────────────────────────────────────────

  @Test
  void constructor_zeroViewportSize_throws() {
    assertThrows(IllegalArgumentException.class, () -> new CameraImpl(0, 0, 0, 100));
    assertThrows(IllegalArgumentException.class, () -> new CameraImpl(0, 0, 100, 0));
  }

  @Test
  void setPosition_updatesXAndY() {
    camera.setPosition(30.0, 45.0);
    assertEquals(30.0, camera.getX(), 1e-9);
    assertEquals(45.0, camera.getY(), 1e-9);
  }

  // ── setViewBoundaries ─────────────────────────────────────────────────────

  @Test
  void setPosition_canBeCalledMultipleTimes() {
    camera.setPosition(1.0, 2.0);
    camera.setPosition(99.0, 88.0);
    assertEquals(99.0, camera.getX(), 1e-9);
    assertEquals(88.0, camera.getY(), 1e-9);
  }

  @Test
  void setViewBoundaries_updatesSize() {
    // After updating size, an object that was outside should become visible
    camera.setPosition(0.0, 0.0);
    Renderable r = makeRenderable(150.0, 50.0);
    assertFalse(camera.isVisible(r));

    camera.setViewBoundaries(200.0, 100.0);
    assertTrue(camera.isVisible(r));
  }

  @Test
  void setViewBoundaries_zeroWidth_throws() {
    assertThrows(IllegalArgumentException.class, () -> camera.setViewBoundaries(0, 100));
  }

  // ── isVisible ─────────────────────────────────────────────────────────────

  @Test
  void setViewBoundaries_negativeHeight_throws() {
    assertThrows(IllegalArgumentException.class, () -> camera.setViewBoundaries(100, -1));
  }

  @Test
  void isVisible_objectInsideViewport_returnsTrue() {
    assertTrue(camera.isVisible(makeRenderable(50.0, 50.0)));
  }

  @Test
  void isVisible_objectAtTopLeft_returnsTrue() {
    assertTrue(camera.isVisible(makeRenderable(0.0, 0.0)));
  }

  @Test
  void isVisible_objectAtBottomRight_returnsTrue() {
    assertTrue(camera.isVisible(makeRenderable(100.0, 100.0)));
  }

  @Test
  void isVisible_objectJustOutsideLeft_returnsFalse() {
    assertFalse(camera.isVisible(makeRenderable(-0.001, 50.0)));
  }

  @Test
  void isVisible_objectJustOutsideRight_returnsFalse() {
    assertFalse(camera.isVisible(makeRenderable(100.001, 50.0)));
  }

  @Test
  void isVisible_objectJustAboveTop_returnsFalse() {
    assertFalse(camera.isVisible(makeRenderable(50.0, -0.001)));
  }

  @Test
  void isVisible_objectJustBelowBottom_returnsFalse() {
    assertFalse(camera.isVisible(makeRenderable(50.0, 100.001)));
  }

  @Test
  void isVisible_null_returnsFalse() {
    assertFalse(camera.isVisible(null));
  }

  @Test
  void isVisible_afterCameraMove_objectPreviouslyOutsideBecomesVisible() {
    Renderable r = makeRenderable(200.0, 50.0);
    assertFalse(camera.isVisible(r));
    camera.setPosition(100.0, 0.0);
    assertTrue(camera.isVisible(r));
  }

  // ── updateBehavior ────────────────────────────────────────────────────────

  @Test
  void isVisible_afterCameraMove_objectPreviouslyInsideBecomesInvisible() {
    Renderable r = makeRenderable(50.0, 50.0);
    assertTrue(camera.isVisible(r));
    camera.setPosition(200.0, 200.0);
    assertFalse(camera.isVisible(r));
  }

  @Test
  void updateBehavior_noBehaviorSet_doesNotThrow() {
    camera.updateBehavior(); // should be a no-op
  }

  @Test
  void updateBehavior_withBehavior_callsBehaviorUpdate() {
    CameraBehavior behavior = mock(CameraBehavior.class);
    camera.setBehavior(behavior);
    camera.updateBehavior();
    verify(behavior).update(camera);
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  @Test
  void updateBehavior_afterSettingBehaviorToNull_doesNotCallOldBehavior() {
    CameraBehavior behavior = mock(CameraBehavior.class);
    camera.setBehavior(behavior);
    camera.setBehavior(null);
    camera.updateBehavior();
    verify(behavior, never()).update(org.mockito.ArgumentMatchers.any());
  }
}
