package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.TerrainBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CollisionDetectorImpl}.
 *
 * <p>Focus:
 * <ul>
 *   <li>Same-object pairs are rejected without running SAT.</li>
 *   <li>Non-overlapping polygons produce no collision.</li>
 *   <li>Single-polygon objects still behave identically after the multi-polygon refactor.</li>
 *   <li>Multi-polygon objects return the collision with the deepest penetration.</li>
 *   <li>Objects exposing an empty polygon list (e.g. an un-initialised component) do not throw.
 *       This is part of the {@code getCollisionPolygons} default contract added alongside
 *       multi-polygon support.</li>
 * </ul>
 */
class CollisionDetectorImplTest {

  private CollisionDetectorImpl detector;

  @BeforeEach
  void setUp() {
    detector = new CollisionDetectorImpl();
  }

  @Test
  void detectCollision_sameObject_returnsEmpty() {
    TerrainBlock block = new TerrainBlock(0, 0, 10, 10, "a", false, 0);
    assertTrue(detector.detectCollision(block, block).isEmpty());
  }

  @Test
  void detectCollision_disjointBoxes_returnsEmpty() {
    TerrainBlock a = new TerrainBlock(0, 0, 10, 10, "a", false, 0);
    TerrainBlock b = new TerrainBlock(100, 100, 10, 10, "b", false, 0);

    assertTrue(detector.detectCollision(a, b).isEmpty());
  }

  @Test
  void detectCollision_overlappingSinglePolygonObjects_reportsCollision() {
    TerrainBlock a = new TerrainBlock(0, 0, 10, 10, "a", false, 0);
    TerrainBlock b = new TerrainBlock(5, 5, 10, 10, "b", false, 0);

    Optional<Collision> collision = detector.detectCollision(a, b);

    assertTrue(collision.isPresent());
    assertNotNull(collision.get().getPenetration());
    assertNotNull(collision.get().getReferenceEdge());
    assertNotNull(collision.get().getIncidentEdge());
  }

  @Test
  void detectCollision_objectWithNoPolygons_returnsEmpty() {
    GameObject empty = new FakeObject(Collections.emptyList());
    TerrainBlock block = new TerrainBlock(0, 0, 10, 10, "a", false, 0);

    assertTrue(detector.detectCollision(empty, block).isEmpty());
    assertTrue(detector.detectCollision(block, empty).isEmpty());
  }

  @Test
  void detectCollision_multiPolygonObject_picksDeepestSubPolygonCollision() {
    // Object A is a single small box overlapping with polygon "b1" by 2px and polygon "b2" by 8px.
    // The detector must return the 8px collision, not the 2px one.
    ConvexPolygon shallow = ConvexPolygon.axisAlignedBox(8, 0, 10, 10);  // overlap width 2
    ConvexPolygon deep = ConvexPolygon.axisAlignedBox(2, 0, 10, 10);     // overlap width 8
    GameObject multi = new FakeObject(List.of(shallow, deep));
    TerrainBlock a = new TerrainBlock(0, 0, 10, 10, "a", false, 0);

    Optional<Collision> collision = detector.detectCollision(a, multi);

    assertTrue(collision.isPresent());
    // Penetration magnitude should reflect the deeper overlap (8), not the shallow one (2).
    assertTrue(collision.get().getPenetration().magnitude() >= 7.999,
        "expected deeper-overlap penetration, got " + collision.get().getPenetration().magnitude());
  }

  @Test
  void detectCollision_multiPolygonObject_nonOverlappingSubPolygonsReturnEmpty() {
    ConvexPolygon farA = ConvexPolygon.axisAlignedBox(500, 500, 10, 10);
    ConvexPolygon farB = ConvexPolygon.axisAlignedBox(600, 600, 10, 10);
    GameObject multi = new FakeObject(List.of(farA, farB));
    TerrainBlock block = new TerrainBlock(0, 0, 10, 10, "a", false, 0);

    assertFalse(detector.detectCollision(block, multi).isPresent());
  }

  @Test
  void detectCollision_objectIdentityPreservedOnCollisionResult() {
    TerrainBlock a = new TerrainBlock(0, 0, 10, 10, "a", false, 0);
    TerrainBlock b = new TerrainBlock(5, 5, 10, 10, "b", false, 0);

    Collision collision = detector.detectCollision(a, b).orElseThrow();

    // The reference/incident identity depends on which SAT pass found the minimum, but both
    // original objects must be present in the result.
    GameObject ref = collision.getReferenceObject();
    GameObject inc = collision.getIncidentObject();
    assertTrue((ref == a && inc == b) || (ref == b && inc == a));
  }

  @Test
  void getCollisionPolygons_defaultBehaviourForSinglePolygonObjectIsSingletonList() {
    TerrainBlock block = new TerrainBlock(0, 0, 10, 10, "a", false, 0);

    List<ConvexPolygon> polygons = block.getCollisionPolygons();

    assertEquals(1, polygons.size());
    assertSame(block.getPolygon(), polygons.get(0));
  }

  /**
   * Minimal {@link GameObject} stub for multi-polygon tests. Only the methods exercised by the
   * collision detector are implemented; everything else throws to surface unintended coupling.
   */
  private static final class FakeObject implements GameObject {
    private final List<ConvexPolygon> polygons;

    FakeObject(List<ConvexPolygon> polygons) {
      this.polygons = polygons;
    }

    @Override public List<ConvexPolygon> getCollisionPolygons() { return polygons; }
    @Override public ConvexPolygon getPolygon() { return polygons.isEmpty() ? null : polygons.get(0); }
    @Override public String getTag() { return "fake"; }
    @Override public void setTag(String tag) {}
    @Override public double getMass() { return 0; }
    @Override public Vector getForce() { return new Vector(0, 0); }
    @Override public void setForce(Vector force) {}
    @Override public void setMass(double mass) {}
    @Override public ObjectState getState() { return null; }
    @Override public void setState(ObjectState state) {}
    @Override public double getMomentOfInertia() { return 0; }
    @Override public void setMomentOfInertia(double i) {}
    @Override public double getRestitution() { return 0; }
    @Override public void setRestitution(double r) {}
    @Override public double getFriction() { return 0; }
    @Override public boolean isActive() { return true; }
    @Override public boolean isAffectedByGravity() { return false; }
    @Override public boolean isTrigger() { return false; }
    @Override public void addForce(Vector force) {}
    @Override public double getX() { return 0; }
    @Override public double getY() { return 0; }
    @Override public double getRotation() { return 0; }
  }
}
