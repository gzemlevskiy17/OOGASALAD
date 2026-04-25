package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import oogasalad.model.engine.internal.joints.RigidJoint;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the "landing explosion" bug: when a dynamic body falls onto static terrain,
 * it should come to rest (or bounce realistically), not accumulate runaway velocity across the
 * 8-iteration joint solver loop.
 *
 * <p>As of the merge that removed {@code VehicleTerrainSupport}, these tests demonstrate the
 * failure mode: {@code GameEngineImpl.step()} calls {@code handleCollisions()} 1 + 8 times per
 * frame, and each call runs the full impulse solver. Residual penetration after positional
 * correction re-triggers impulses on already-corrected velocities, and velocities amplify
 * exponentially.
 */
class LandingStabilityTest {

  private static final double DT = 1.0 / 60.0;
  private static final int SIM_STEPS = 180;       // 3 seconds at 60 Hz
  private static final double GRAVITY = 980.0;    // must match GameEngineImpl
  private static final double TERMINAL_SPEED_BOUND = 5 * GRAVITY; // 5s of free-fall equivalent

  private static DynamicComponent fallingBox(double x, double y, double size, double mass) {
    ConvexPolygon poly = ConvexPolygon.axisAlignedBox(x - size / 2, y - size / 2, size, size);
    double moi = mass * (size * size + size * size) / 12.0;
    DynamicComponent body =
        new DynamicComponent(x, y, 0, poly, "", 0.3, mass, moi, 0.2, true) {
          @Override
          public void updatePolygon() {
            setPolygon(ConvexPolygon.axisAlignedBox(
                x() - size / 2, y() - size / 2, size, size));
          }

          @Override
          public boolean isTrigger() {
            return false;
          }
        };
    body.activate();
    return body;
  }

  private static TerrainBlock groundAt(double topY, double width) {
    TerrainBlock ground =
        new TerrainBlock(-width / 2, topY, width, 200, "t.png", false, 0.3, 0.2);
    ground.activate();
    return ground;
  }

  /**
   * A single dynamic box falls onto flat static terrain. After enough settle time, its speed should
   * be bounded and roughly consistent with gravity + bounce, not runaway-amplified.
   */
  @Test
  void fallingBox_landsWithBoundedSpeed() {
    WorldImpl world = new WorldImpl();
    DynamicComponent body = fallingBox(0, 0, 40, 2.0);
    TerrainBlock ground = groundAt(200, 2000);
    world.addComponent(body);
    world.addComponent(ground);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
    }

    Vector v = body.getDynamicState().getLinearVelocity();
    double speed = Math.hypot(v.x(), v.y());
    assertTrue(speed < TERMINAL_SPEED_BOUND,
        "Body speed after landing should stay bounded. Got " + speed
            + " px/s (bound " + TERMINAL_SPEED_BOUND + ")");
  }

  /**
   * A body starting above flat terrain with zero horizontal velocity should not drift sideways. Any
   * significant x-drift is a signature of the impulse-reapplication bug: angled contact normals at
   * box corners leak normal impulses into the tangent direction, and 9 applications per frame
   * amplify them.
   */
  @Test
  void fallingBox_noHorizontalDrift() {
    WorldImpl world = new WorldImpl();
    DynamicComponent body = fallingBox(0, 0, 40, 2.0);
    TerrainBlock ground = groundAt(200, 2000);
    world.addComponent(body);
    world.addComponent(ground);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
    }

    double xDrift = Math.abs(body.getStaticState().getLinearPosition().x());
    assertTrue(xDrift < 50.0,
        "Body should not drift sideways when landing on flat ground. Got x=" + xDrift);
  }

  /**
   * Tight bound on horizontal velocity — should be near zero after settling on flat ground.
   */
  @Test
  void fallingBox_horizontalVelocityStaysSmall() {
    WorldImpl world = new WorldImpl();
    DynamicComponent body = fallingBox(0, 0, 40, 2.0);
    TerrainBlock ground = groundAt(200, 2000);
    world.addComponent(body);
    world.addComponent(ground);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
    }

    double vx = Math.abs(body.getDynamicState().getLinearVelocity().x());
    assertTrue(vx < 100.0,
        "Body should have near-zero horizontal velocity after settling. Got vx=" + vx);
  }

  /**
   * Two dynamic bodies linked by a rigid joint fall together and land on terrain. This is the
   * actual scenario that blows up in the running game: the joint snaps positions between each of
   * the 8 handleCollisions iterations, re-introducing penetration and re-triggering impulses on
   * already-corrected velocities.
   */
  @Test
  void jointedPair_landsWithBoundedSpeed() {
    WorldImpl world = new WorldImpl();
    DynamicComponent wheel = fallingBox(0, 0, 40, 2.0);
    DynamicComponent chassis = fallingBox(0, -40, 40, 2.0);
    TerrainBlock ground = groundAt(200, 2000);
    world.addComponent(wheel);
    world.addComponent(chassis);
    world.addComponent(ground);
    world.addJoint(new RigidJoint(wheel, chassis));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
    }

    Vector wv = wheel.getDynamicState().getLinearVelocity();
    Vector cv = chassis.getDynamicState().getLinearVelocity();
    double maxSpeed = Math.max(Math.hypot(wv.x(), wv.y()), Math.hypot(cv.x(), cv.y()));
    assertTrue(maxSpeed < TERMINAL_SPEED_BOUND,
        "Jointed pair speed should stay bounded. Got " + maxSpeed
            + " px/s (bound " + TERMINAL_SPEED_BOUND + ")");
  }

  /**
   * Full mini-vehicle: chassis + 2 wheels attached via revolute joints, landing on terrain. Matches
   * what {@code VehicleFactory} produces for wheel/non-wheel cardinal neighbors. Prints peak speed
   * each step so we can see whether impulses actually blow up.
   */
  @Test
  void miniVehicle_landsWithoutExplosion() {
    WorldImpl world = new WorldImpl();
    double size = 40;
    DynamicComponent chassis = fallingBox(0, 0, size, 5.0);
    DynamicComponent leftWheel = fallingBox(-size, size, size, 2.0);
    DynamicComponent rightWheel = fallingBox(size, size, size, 2.0);
    TerrainBlock ground = groundAt(200, 4000);
    world.addComponent(chassis);
    world.addComponent(leftWheel);
    world.addComponent(rightWheel);
    world.addComponent(ground);
    // Revolute joint anchors: chassis bottom-left edge to left wheel top-right, etc.
    world.addJoint(new RevoluteJoint(chassis, leftWheel,
        new Vector(-size / 2, size / 2), new Vector(size / 2, -size / 2)));
    world.addJoint(new RevoluteJoint(chassis, rightWheel,
        new Vector(size / 2, size / 2), new Vector(-size / 2, -size / 2)));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    double peakSpeed = 0;
    double peakXDrift = 0;
    int firstContactStep = -1;
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
      Vector cv = chassis.getDynamicState().getLinearVelocity();
      double speed = Math.hypot(cv.x(), cv.y());
      if (speed > peakSpeed) {
        peakSpeed = speed;
      }
      double xd = Math.abs(chassis.getStaticState().getLinearPosition().x());
      if (xd > peakXDrift) {
        peakXDrift = xd;
      }
      if (firstContactStep < 0
          && chassis.getStaticState().getLinearPosition().y() + size / 2 > 150) {
        firstContactStep = i;
      }
    }
    System.out.println("[miniVehicle] peakSpeed=" + peakSpeed + " peakXDrift=" + peakXDrift
        + " firstContactStep=" + firstContactStep);

    assertTrue(peakSpeed < TERMINAL_SPEED_BOUND,
        "Mini-vehicle peak speed should stay bounded. Got " + peakSpeed);
    assertTrue(peakXDrift < 100.0,
        "Mini-vehicle shouldn't launch sideways. peak x=" + peakXDrift);
  }

  /**
   * Diagnostic check for the observed "explodes upward" behavior on first ground contact.
   */
  @Test
  void miniVehicle_firstContactDoesNotInjectLargeUpwardVelocity() {
    WorldImpl world = new WorldImpl();
    double size = 40;
    DynamicComponent chassis = fallingBox(0, 0, size, 5.0);
    DynamicComponent leftWheel = fallingBox(-size, size, size, 2.0);
    DynamicComponent rightWheel = fallingBox(size, size, size, 2.0);
    TerrainBlock ground = groundAt(200, 4000);
    world.addComponent(chassis);
    world.addComponent(leftWheel);
    world.addComponent(rightWheel);
    world.addComponent(ground);
    world.addJoint(new RevoluteJoint(chassis, leftWheel,
        new Vector(-size / 2, size / 2), new Vector(size / 2, -size / 2)));
    world.addJoint(new RevoluteJoint(chassis, rightWheel,
        new Vector(size / 2, size / 2), new Vector(-size / 2, -size / 2)));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    boolean contactSeen = false;
    double maxUpwardVelocityAfterContact = 0.0;
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
      double chassisBottom = chassis.getStaticState().getLinearPosition().y() + size / 2;
      if (chassisBottom >= 200) {
        contactSeen = true;
      }
      if (contactSeen) {
        maxUpwardVelocityAfterContact = Math.max(maxUpwardVelocityAfterContact,
            -chassis.getDynamicState().getLinearVelocity().y());
      }
    }

    assertTrue(contactSeen, "Expected mini-vehicle to contact terrain during simulation.");
    assertTrue(maxUpwardVelocityAfterContact < 600.0,
        "Large upward launch detected after contact: " + maxUpwardVelocityAfterContact);
  }

  /**
   * Same jointed pair shouldn't drift sideways off the landing point either.
   */
  @Test
  void jointedPair_noHorizontalLaunch() {
    WorldImpl world = new WorldImpl();
    DynamicComponent wheel = fallingBox(0, 0, 40, 2.0);
    DynamicComponent chassis = fallingBox(0, -40, 40, 2.0);
    TerrainBlock ground = groundAt(200, 2000);
    world.addComponent(wheel);
    world.addComponent(chassis);
    world.addComponent(ground);
    world.addJoint(new RigidJoint(wheel, chassis));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
    }

    double wx = Math.abs(wheel.getStaticState().getLinearPosition().x());
    double cx = Math.abs(chassis.getStaticState().getLinearPosition().x());
    assertTrue(Math.max(wx, cx) < 100.0,
        "Jointed pair shouldn't launch sideways. wheel x=" + wx + " chassis x=" + cx);
  }
}
