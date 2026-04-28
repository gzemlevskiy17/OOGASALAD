package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.joints.RigidJoint;
import org.junit.jupiter.api.Test;

/**
 * Contract-style tests for the physics path in {@link GameEngineImpl}. These are written to catch
 * broad-phase mistakes (e.g. calling narrow-phase twice per object pair) and obvious integration or
 * collision-resolution regressions. Replace tolerances if your project uses different units/scale.
 */
class GameEnginePhysicsContractTest {

  private static final double EPS = 1e-5;
  private static final double G = 980.0;
  private static final double DT = 1.0 / 60.0;

  @Test
  void broadPhase_skipsSeparatedPairs_twoDynamicPlusFloor() {
    World world = new WorldImpl();
    TerrainBlock floor = new TerrainBlock(0, 0, 400, 1, "floor.png", false, 0.4, "#888888", 0.0);
    TestBox a = new TestBox(5, 4, 1, 1, 1.0, 0.0, true);
    TestBox b = new TestBox(200, 4, 1, 1, 1.0, 0.0, true);
    world.addComponent(floor);
    world.addComponent(a);
    world.addComponent(b);

    CountingCollisionDetector counting = new CountingCollisionDetector(new CollisionDetectorImpl());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    assertEquals(0, counting.getNarrowPhaseCallCount());
  }

  @Test
  void broadPhase_skipsSeparatedPairs_fiveIsolatedDynamicBodies() {
    World world = new WorldImpl();
    for (int i = 0; i < 5; i++) {
      // Spaced 50 units — no broad-phase culling, but no overlap either.
      TestBox body = new TestBox(10 + i * 50, 10, 1, 1, 1.0, 0.0, true);
      world.addComponent(body);
    }

    CountingCollisionDetector counting = new CountingCollisionDetector(new CollisionDetectorImpl());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    assertEquals(0, counting.getNarrowPhaseCallCount());
  }

  @Test
  void broadPhase_invokesNarrowPhaseForOverlappingAabbs() {
    World world = new WorldImpl();
    TerrainBlock floor = new TerrainBlock(0, 0, 10, 10, "floor.png", false, 0.4, "#888888", 0.0);
    TestBox body = new TestBox(5, 5, 1, 1, 1.0, 0.0, false);
    world.addComponent(floor);
    world.addComponent(body);

    CountingCollisionDetector counting = new CountingCollisionDetector((a, b) -> Optional.empty());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    assertTrue(counting.getNarrowPhaseCallCount() > 0,
        "Overlapping AABBs should trigger at least one narrow-phase check");
  }

  @Test
  void collisionFilter_maskExcludesPairBeforeNarrowPhase() {
    World world = new WorldImpl();
    TerrainBlock floor = new TerrainBlock(0, 0, 10, 10, "floor.png", false, 0.4, "#888888", 0.0);
    TestBox body = new TestBox(5, 5, 1, 1, 1.0, 0.0, false);
    world.addComponent(floor);
    world.addComponent(body);

    CountingCollisionDetector counting = new CountingCollisionDetector((a, b) -> Optional.empty());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    assertEquals(0, counting.getNarrowPhaseCallCount());
  }

  @Test
  void collideConnected_allowsJointedPairToReachNarrowPhase() {
    WorldImpl world = new WorldImpl();
    TestBox objectA = new TestBox(0, 0, 1, 1, 1.0, 0.0, false);
    TestBox objectB = new TestBox(0, 0, 1, 1, 1.0, 0.0, false);
    RigidJoint joint = new RigidJoint(objectA, objectB);
    joint.setCollideConnected(true);
    world.addComponent(objectA);
    world.addComponent(objectB);
    world.addJoint(joint);

    CountingCollisionDetector counting = new CountingCollisionDetector((a, b) -> Optional.empty());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    assertTrue(counting.getNarrowPhaseCallCount() > 0,
        "Jointed pair with collideConnected=true should reach narrow phase");
  }

  // --- Tests: broad-phase AABB culling ---

  @Test
  void firstStepOfFreeFall_producesDownwardMotion() {
    World world = new WorldImpl();
    TestBox body = new TestBox(0, 100, 1, 1, 1.0, 0.0, true);
    world.addComponent(body);

    CollisionDetector noCollisions = (x, y) -> Optional.empty();
    GameEngineImpl engine = new GameEngineImpl(world, noCollisions);
    engine.step(DT);

    double ay = body.getDynamicState().getLinearAcceleration().y();
    assertTrue(ay > 900 && ay <= G,
        "Acceleration should be close to gravity (with drag), got " + ay);
    assertTrue(body.getDynamicState().getLinearVelocity().y() > 0,
        "Body should be falling downward");
    assertTrue(body.getStaticState().getLinearPosition().y() > 100,
        "Body should have moved down from starting position");
  }

  @Test
  void wellSeparatedFallingBodies_doNotDriftInX() {
    World world = new WorldImpl();
    TestBox left = new TestBox(-80, 50, 1, 1, 1.0, 0.0, true);
    TestBox right = new TestBox(80, 50, 1, 1, 1.0, 0.0, true);
    world.addComponent(left);
    world.addComponent(right);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < 120; i++) {
      engine.step(DT);
    }

    assertEquals(-80, left.getStaticState().getLinearPosition().x(), 1e-2);
    assertEquals(80, right.getStaticState().getLinearPosition().x(), 1e-2);
  }

  // --- Test: free fall matches semi-implicit Euler with engine force layout ---

  @Test
  void boxFallingOnFlatFloor_endsUpNearRestingHeightAndBoundedVerticalVelocity() {
    World world = new WorldImpl();
    // Floor top surface at y = 1.
    TerrainBlock floor = new TerrainBlock(0, 0, 400, 1, "floor.png", false, 0.4, "#888888", 0.0);
    TestBox body = new TestBox(120, 50, 1, 1, 1.0, 0.0, true);
    world.addComponent(floor);
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    for (int i = 0; i < 600; i++) {
      engine.step(DT);
    }

    double y = body.getStaticState().getLinearPosition().y();
    // Bottom of 1x1 AABB: resting with bottom on y=1.
    assertTrue(y >= 0.8 && y <= 1.4, "Expected roughly resting on y≈1, got y=" + y);
    assertTrue(
        Math.abs(body.getDynamicState().getLinearVelocity().y()) < 2.0,
        "Unbounded vertical velocity after settle: " + body.getDynamicState().getLinearVelocity()
            .y());
  }

  @Test
  void fastBody_doesNotTunnelThroughWall() {
    World world = new WorldImpl();
    TestBox body = new TestBox(0, 5, 1, 1, 1.0, 0.0, false);
    body.getDynamicState().setLinearVelocity(new oogasalad.model.engine.api.Vector(1000, 0));
    TerrainBlock wall = new TerrainBlock(10, 0, 1, 20, "wall.png", false, 0.4, "#888888", 0.0);
    world.addComponent(body);
    world.addComponent(wall);

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    engine.step(DT);

    assertTrue(body.getStaticState().getLinearPosition().x() <= 11.0,
        "Fast body should not tunnel far through the wall, got x="
            + body.getStaticState().getLinearPosition().x());
  }

  // --- Test: no lateral motion when the only net force is vertical gravity ---

  /**
   * {@link PigComponent} is used as the concrete body type because it already implements
   * {@code updatePolygon} for an axis-aligned hitbox. We only override the polygon update to
   * control size for tests; behavior is a generic dynamic box in world space.
   */
  private static final class TestBox extends PigComponent {

    private final double width;
    private final double height;

    TestBox(
        double x,
        double y,
        double width,
        double height,
        double mass,
        double restitution,
        boolean gravity) {
      super(
          x,
          y,
          0,
          ConvexPolygon.axisAlignedBox(x, y, width, height),
          "test-box",
          0.0,
          mass,
          1.0,
          restitution,
          gravity,
          100.0,
          "test-box");
      this.width = width;
      this.height = height;
    }

    @Override
    public void updatePolygon() {
      setPolygon(ConvexPolygon.axisAlignedBox(x(), y(), width, height));
    }
  }

  // --- Test: inelastic rest on a floor (relaxed bounds; will fail on obvious explosions) ---

  /**
   * Forwards to a real implementation while counting how often narrow-phase is invoked.
   */
  private static final class CountingCollisionDetector implements CollisionDetector {

    private final CollisionDetector inner;
    private final AtomicInteger narrowPhaseCalls = new AtomicInteger(0);

    CountingCollisionDetector(CollisionDetector inner) {
      this.inner = inner;
    }

    int getNarrowPhaseCallCount() {
      return narrowPhaseCalls.get();
    }

    @Override
    public Optional<Collision> detectCollision(GameObject objectA, GameObject objectB) {
      narrowPhaseCalls.incrementAndGet();
      return inner.detectCollision(objectA, objectB);
    }
  }

}