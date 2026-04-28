package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TerrainSpline}.
 *
 * <p>Focus: invariants that downstream code depends on — the component is tagged as terrain, is
 * static, exposes every tessellated slab via {@link TerrainSpline#getCollisionPolygons()}, reports
 * its bounding box to the baseline for world-bounds/rendering code, and gracefully defaults
 * null color/sprite to usable fallbacks.
 */
class TerrainSplineTest {

  private static final double TOLERANCE = 1e-6;
  private static final double BASELINE = 500.0;

  private ParabolaSpline spline;

  @BeforeEach
  void setUp() {
    spline = new ParabolaSpline(
        List.of(new Vector(10, 100), new Vector(200, 120), new Vector(400, 90)),
        BASELINE);
  }

  @Test
  void constructor_nullSpline_throws() {
    NullPointerException exception = assertThrows(NullPointerException.class,
        () -> new TerrainSpline(null, "", false, 0.5, "#fff"));
    assertNotNull(exception);
  }

  @Test
  void constructor_tagsAsTerrain() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);
    assertEquals(DefaultTag.TERRAIN.value(), terrain.getTag());
  }

  @Test
  void constructor_isNotAffectedByGravity() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);
    assertFalse(terrain.isAffectedByGravity());
  }

  @Test
  void getSpline_returnsConstructorArgument() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);
    assertSame(spline, terrain.getSpline());
  }

  @Test
  void getCollisionPolygons_nonEmpty_exposesTessellatedSlabs() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);

    List<ConvexPolygon> slabs = terrain.getCollisionPolygons();

    assertEquals(12, slabs.size()); // 2 segments × 6 resolution
  }

  @Test
  void getPolygon_returnsFirstSlab_forLegacyCallers() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);

    ConvexPolygon first = terrain.getCollisionPolygons().get(0);
    assertSame(first, terrain.getPolygon());
  }

  @Test
  void getX_getY_matchSplineBoundingBoxTopLeft() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);

    // min x across knots is 10, min y is 90
    assertEquals(10.0, terrain.x(), TOLERANCE);
    assertEquals(90.0, terrain.y(), TOLERANCE);
  }

  @Test
  void getWidth_matchesMaxMinusMinX() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);
    assertEquals(400.0 - 10.0, terrain.getWidth(), TOLERANCE);
  }

  @Test
  void getHeight_extendsToBaseline_forVisualBoundingBox() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);
    assertEquals(BASELINE - 90.0, terrain.getHeight(), TOLERANCE);
  }

  @Test
  void defaultColorAndSprite_areReasonable_whenConvenienceConstructorUsed() {
    TerrainSpline terrain = new TerrainSpline(spline, 0.5);

    assertNotNull(terrain.getColor());
    assertNotNull(terrain.getSprite());
    assertFalse(terrain.isTiled());
  }

  @Test
  void explicitAttributes_roundTripThroughGetters() {
    TerrainSpline terrain = new TerrainSpline("dirt", spline, "assets/dirt.png", true, 0.7,
        "#00ff00", "#123456");

    assertEquals("assets/dirt.png", terrain.getSprite());
    assertTrue(terrain.isTiled());
    assertEquals("#00ff00", terrain.getColor());
    assertEquals("#123456", terrain.getTrackColor());
    assertEquals(0.7, terrain.getFriction(), TOLERANCE);
  }

  @Test
  void nullColor_andNullSprite_fallBackToDefaults() {
    TerrainSpline terrain = new TerrainSpline(spline, null, false, 0.5, null);

    assertNotNull(terrain.getColor());
    assertNotNull(terrain.getSprite());
  }
}
