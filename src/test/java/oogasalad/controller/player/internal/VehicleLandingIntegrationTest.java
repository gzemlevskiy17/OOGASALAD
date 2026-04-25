package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.controller.player.internal.parts.VehiclePartRegistry;
import oogasalad.controller.player.internal.parts.VehiclePartSpec;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import org.junit.jupiter.api.Test;

/**
 * End-to-end landing tests for vehicles built by {@link VehicleFactory}. Unlike
 * {@code LandingStabilityTest} — which uses anonymous {@code DynamicComponent} subclasses with
 * custom {@code updatePolygon()} — this test drives the real subclasses
 * ({@code StructuralComponent}, wheel components) through the real factory code path. Some bugs
 * only show up with the real parts:
 *
 * <ul>
 *   <li>Initial polygon placement mismatch: {@code StructuralPartSpec.create()} passes
 *       {@code (x, y)} as the <em>corner</em> to {@code axisAlignedBox}, but
 *       {@code VehicleComponent.updatePolygon()} treats {@code (x, y)} as the <em>center</em>.
 *       The polygon jumps by half the body size on the first {@code updatePolygon()} call,
 *       which can manifest as a phantom penetration against terrain on the first frame.</li>
 *   <li>Real restitution (0.5 for frames) is larger than the 0.2 used in the simpler test, so
 *       bounce energy persists longer through the 9× handleCollisions() loop per frame.</li>
 *   <li>Multi-body joint networks (cardinal + anti-rotation + triangle chords) interact with
 *       each iteration of the joint solver loop.</li>
 * </ul>
 */
class VehicleLandingIntegrationTest {

  private static final double DT = 1.0 / 60.0;
  private static final double CELL_SIZE = 60.0;
  private static final int SIM_STEPS = 240; // 4 seconds at 60 Hz
  private static final double START_X = 0.0;
  private static final double START_Y = 0.0;

  /**
   * Exposes the center/corner convention mismatch between
   * {@link oogasalad.controller.player.internal.parts.StructuralPartSpec#create} and
   * {@link oogasalad.model.engine.internal.components.VehicleComponent#updatePolygon}.
   *
   * <p>Immediately after {@code create(x, y)}, the polygon is built from {@code (x, y)} as the
   * top-left corner. After one call to {@code updatePolygon()} (which the engine does every frame),
   * the polygon is rebuilt from {@code (x, y)} as the center. Those two polygons differ by a
   * half-body-size shift.
   *
   * <p><strong>Expected to FAIL today.</strong>
   */
  @Test
  void structuralPart_polygonShouldNotJumpAfterFirstUpdatePolygon() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    VehiclePartSpec spec = registry.get("Frame");
    GameComponent part = spec.create(100, 100);
    List<Vector> initial = new ArrayList<>(part.getPolygon().points());

    part.updatePolygon();
    List<Vector> afterUpdate = part.getPolygon().points();

    assertEquals(initial, afterUpdate,
        "Polygon should not shift between construction and first updatePolygon(). "
            + "Initial corner: " + initial.get(0)
            + " After updatePolygon: " + afterUpdate.get(0));
  }

  @Test
  void wheelPart_usesRoundColliderBeforeAndAfterPolygonUpdate() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    VehiclePartSpec spec = registry.get("Wood Wheel");
    GameComponent wheel = spec.create(100, 100);

    assertTrue(wheel.getPolygon().points().size() > 5,
        "Wheel collider should use a regular polygon rather than a box");
    wheel.updatePolygon();

    assertTrue(wheel.getPolygon().points().size() > 5,
        "Wheel updatePolygon should preserve the round collider");
  }

  /**
   * Build a minimal 2-wheel vehicle (1 frame + 2 wheels) with {@link VehicleFactory}, drop it onto
   * flat terrain, and verify no dynamic body ever reaches a wildly large upward velocity.
   *
   * <p>The vehicle spawns in free space above the ground so it goes through a real fall
   * → landing → settle sequence. Peak upward speed is tracked across every frame of every dynamic
   * body so the assertion catches a blow-up on <em>any</em> impulse iteration.
   */
  @Test
  void twoWheeledVehicle_builtByFactory_landsWithoutHugeUpwardImpulse() {
    WorldImpl world = new WorldImpl();
    double groundTop = 600;
    TerrainBlock ground = new TerrainBlock(-2000, groundTop, 4000, 200, "", false, 0.3, 0.2);
    ground.activate();
    world.addComponent(ground);

    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(0, 0, "Frame"),
        new PlacedComponent(1, -1, "Wood Wheel"),
        new PlacedComponent(1, 1, "Wood Wheel")));
    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    for (GameObject o : world.getObjects()) {
      if (o instanceof GameComponent gc) {
        gc.activate();
      }
    }

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());

    double peakUpwardSpeed = 0;
    double peakSpeed = 0;
    double deepestY = Double.NEGATIVE_INFINITY;
    int peakFrame = -1;
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
      for (GameObject o : world.getObjects()) {
        if (o instanceof DynamicComponent d) {
          Vector v = d.getDynamicState().getLinearVelocity();
          double upward = -v.y();
          double speed = Math.hypot(v.x(), v.y());
          if (upward > peakUpwardSpeed) {
            peakUpwardSpeed = upward;
            peakFrame = i;
          }
          if (speed > peakSpeed) {
            peakSpeed = speed;
          }
          if (d.y() > deepestY) {
            deepestY = d.y();
          }
        }
      }
    }
    System.out.println("[twoWheeledVehicle] peakUpwardSpeed=" + peakUpwardSpeed
        + " peakSpeed=" + peakSpeed + " peakFrame=" + peakFrame
        + " deepestY=" + deepestY + " groundTop=" + groundTop);

    assertTrue(deepestY > groundTop - 100,
        "Vehicle never reached ground during sim. deepestY=" + deepestY
            + " groundTop=" + groundTop);
    assertTrue(peakUpwardSpeed < 2000,
        "Vehicle gained implausibly large upward velocity: " + peakUpwardSpeed
            + " px/s at frame " + peakFrame);
    assertTrue(peakSpeed < 5000,
        "Vehicle peak total speed is implausibly large: " + peakSpeed);
  }

  /**
   * Drop the same 3-part vehicle from higher, so it hits the ground with ≈2× more velocity. Tracks
   * upward impulse on the frame immediately after any body's collider reaches {@code groundTop}.
   * The real game symptom is "huge upward impulse on landing" — that appears within one or two
   * frames of first contact, so this narrow window is the right place to catch it.
   */
  @Test
  void twoWheeledVehicle_highDrop_firstContactFrame_hasBoundedUpwardImpulse() {
    WorldImpl world = new WorldImpl();
    double groundTop = 1500;
    double dropStartY = 0;
    TerrainBlock ground = new TerrainBlock(-2000, groundTop, 4000, 200, "", false, 0.3, 0.2);
    ground.activate();
    world.addComponent(ground);

    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(0, 0, "Frame"),
        new PlacedComponent(1, -1, "Wood Wheel"),
        new PlacedComponent(1, 1, "Wood Wheel")));
    new VehicleFactory().buildVehicle(layout, world, START_X, dropStartY);
    for (GameObject o : world.getObjects()) {
      if (o instanceof GameComponent gc) {
        gc.activate();
      }
    }

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());

    boolean contactSeen = false;
    double maxUpwardAfterContact = 0;
    int contactFrame = -1;
    for (int i = 0; i < SIM_STEPS * 2; i++) {
      engine.step(DT);
      for (GameObject o : world.getObjects()) {
        if (o instanceof DynamicComponent d) {
          double bottom = d.y() + CELL_SIZE / 2;
          if (bottom >= groundTop) {
            if (!contactSeen) {
              contactSeen = true;
              contactFrame = i;
            }
            double upward = -d.getDynamicState().getLinearVelocity().y();
            if (upward > maxUpwardAfterContact) {
              maxUpwardAfterContact = upward;
            }
          }
        }
      }
    }

    System.out.println("[highDrop] contactFrame=" + contactFrame
        + " maxUpwardAfterContact=" + maxUpwardAfterContact);
    assertTrue(contactSeen, "Expected vehicle to contact ground during sim");
    assertTrue(maxUpwardAfterContact < 1500,
        "Upward velocity after first contact is implausibly large: " + maxUpwardAfterContact);
  }
}
