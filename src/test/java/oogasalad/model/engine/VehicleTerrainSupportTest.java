package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VehicleTerrainSupport}, focusing on the new {@link TerrainSpline} support.
 *
 * <p>This utility snaps the vehicle assembly upward when its lowest vertex sits below the deepest
 * ground patch under its x-span. The tests exercise:
 * <ul>
 *   <li>No-op on empty worlds, worlds without terrain, and worlds without a vehicle assembly.</li>
 *   <li>Spline-based terrain is treated as a ground source (regression for the "fall through the
 *       spline" fix).</li>
 *   <li>Vehicle already above the surface is not moved.</li>
 *   <li>When block and spline both exist under the vehicle, the deeper surface wins.</li>
 * </ul>
 */
class VehicleTerrainSupportTest {

  private static final double TOLERANCE = 1e-6;

  @Test
  void keepAssembly_emptyWorld_doesNothing() {
    FakeWorld world = new FakeWorld();
    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);
    // No exceptions, nothing to assert.
  }

  @Test
  void keepAssembly_noTerrain_leavesVehicleAlone() {
    FakeWorld world = new FakeWorld();
    TestBody vehicle = TestBody.vehicle(0, 200, 40, 20);
    world.addComponent(vehicle);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    assertEquals(200.0, vehicle.getY(), TOLERANCE);
  }

  @Test
  void keepAssembly_noVehicle_leavesTerrainAlone() {
    FakeWorld world = new FakeWorld();
    TerrainBlock block = new TerrainBlock(0, 500, 200, 20, "g", false, 0.5);
    world.addComponent(block);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    assertEquals(0.0, block.getX(), TOLERANCE);
    assertEquals(500.0, block.getY(), TOLERANCE);
  }

  @Test
  void keepAssembly_vehicleSinkingIntoBlock_isSnappedUpToSurface() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TerrainBlock(0, 500, 200, 100, "g", false, 0.5));
    // Vehicle box is 40 wide, 20 tall — its polygon points are centered on (x=100, y=600), which
    // puts its bottom edge at y=610, well below the block top at y=500.
    TestBody vehicle = TestBody.vehicleAt(100, 600, 40, 20);
    world.addComponent(vehicle);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    // The bottom edge should now sit just at/just-past the block's top (500 + SURFACE_EPSILON).
    double bottomY = vehicle.getY() + 10; // half of 20
    assertTrue(bottomY <= 501.0, "expected vehicle bottom (<=501.0), got " + bottomY);
    assertTrue(bottomY >= 500.0 - 1e-6, "vehicle should not overshoot above the surface");
  }

  @Test
  void keepAssembly_vehicleAboveBlock_isNotMoved() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TerrainBlock(0, 500, 200, 100, "g", false, 0.5));
    TestBody vehicle = TestBody.vehicleAt(100, 100, 40, 20);
    world.addComponent(vehicle);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    assertEquals(100.0, vehicle.getY(), TOLERANCE);
  }

  @Test
  void keepAssembly_vehicleSinkingIntoSpline_isSnappedUpToSlabTop() {
    // Horizontal spline at y=400; slabs run from y=400 down ~200 px, and their tops are what the
    // support utility should latch onto.
    ParabolaSpline flat = new ParabolaSpline(
        List.of(new Vector(0, 400), new Vector(400, 400)), 900, 4);
    TerrainSpline terrain = new TerrainSpline(flat, 0.5);

    FakeWorld world = new FakeWorld();
    world.addComponent(terrain);
    // Drop the vehicle assembly centered at (200, 500) so its box straddles some slabs with
    // plenty of overlap.
    TestBody vehicle = TestBody.vehicleAt(200, 500, 40, 20);
    world.addComponent(vehicle);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    double bottomY = vehicle.getY() + 10;
    assertTrue(bottomY <= 401.0, "spline top should lift vehicle, bottom=" + bottomY);
    assertTrue(bottomY >= 400.0 - 1e-6, "vehicle should not overshoot above the spline");
  }

  @Test
  void keepAssembly_blockAndSplineBothUnderVehicle_usesDeepestTop() {
    // Block top at y=500, spline top at y=400. With +Y downward, y=500 is "deeper" (lower), so
    // the vehicle should be corrected to sit just above y=500, not y=400.
    ParabolaSpline flat = new ParabolaSpline(
        List.of(new Vector(0, 400), new Vector(400, 400)), 900, 4);
    TerrainSpline terrainSpline = new TerrainSpline(flat, 0.5);

    FakeWorld world = new FakeWorld();
    world.addComponent(new TerrainBlock(0, 500, 200, 100, "g", false, 0.5));
    world.addComponent(terrainSpline);
    TestBody vehicle = TestBody.vehicleAt(100, 700, 40, 20);
    world.addComponent(vehicle);

    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    double bottomY = vehicle.getY() + 10;
    assertTrue(bottomY > 400.0 + 1.0,
        "vehicle should not hang at spline top when a deeper block exists, bottom=" + bottomY);
    assertTrue(bottomY <= 501.0,
        "vehicle should rest on the deeper block top, bottom=" + bottomY);
  }

  @Test
  void keepAssembly_pigsCountAsAssemblyMembers_andAreTranslatedWithVehicle() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TerrainBlock(0, 500, 200, 100, "g", false, 0.5));
    TestBody vehicle = TestBody.vehicleAt(100, 600, 40, 20);
    TestBody pig = TestBody.pigAt(120, 590, 20, 20);
    world.addComponent(vehicle);
    world.addComponent(pig);

    double pigYBefore = pig.getY();
    VehicleTerrainSupport.keepVehicleAssemblyAboveTerrainSurface(world);

    // Pig moves by the same delta as the vehicle
    assertEquals(vehicle.getY() - 600.0, pig.getY() - pigYBefore, TOLERANCE);
  }

  // --- Test doubles ---

  /** Minimal {@link GameComponent} used as a vehicle/pig stand-in with an axis-aligned polygon. */
  private static final class TestBody extends GameComponent {

    private TestBody(double cx, double cy, double width, double height, String tag) {
      super(cx, cy, 0, 1, 0);
      setTag(tag);
      setPolygon(ConvexPolygon.axisAlignedBox(cx - width / 2, cy - height / 2, width, height));
    }

    static TestBody vehicle(double cx, double cy, double w, double h) {
      return new TestBody(cx, cy, w, h, DefaultTag.VEHICLE.value());
    }

    static TestBody vehicleAt(double cx, double cy, double w, double h) {
      return vehicle(cx, cy, w, h);
    }

    static TestBody pigAt(double cx, double cy, double w, double h) {
      return new TestBody(cx, cy, w, h, DefaultTag.PIGS.value());
    }

    @Override
    public void updatePolygon() {
      // Keep polygon synchronized with state after the support utility translates the body.
      double cx = getX();
      double cy = getY();
      double width = getPolygon().getPoints().stream().mapToDouble(Vector::x).max().orElseThrow()
          - getPolygon().getPoints().stream().mapToDouble(Vector::x).min().orElseThrow();
      double height = getPolygon().getPoints().stream().mapToDouble(Vector::y).max().orElseThrow()
          - getPolygon().getPoints().stream().mapToDouble(Vector::y).min().orElseThrow();
      setPolygon(ConvexPolygon.axisAlignedBox(cx - width / 2, cy - height / 2, width, height));
    }
  }

  private static final class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();
    @Override public List<GameComponent> getComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getAllComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getActiveComponents() { return List.copyOf(components); }
    @Override public List<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent c) { components.add(c); }
    @Override public void removeComponent(GameComponent c) { components.remove(c); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
  }
}
