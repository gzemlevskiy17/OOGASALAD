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
import org.junit.jupiter.api.Test;

/**
 * Contract-style tests for the physics path in {@link GameEngineImpl}. These are written to catch
 * broad-phase mistakes (e.g. calling narrow-phase twice per object pair) and obvious integration or
 * collision-resolution regressions. Replace tolerances if your project uses different units/scale.
 */
class GameEnginePhysicsContractTest {

  private static final double EPS = 1e-5;
  /**
   * Must match {@link GameEngineImpl}'s gravity force-per-mass in Y (see gravity vector in
   * engine).
   */
  private static final double G = 980.0;
  private static final double DT = 1.0 / 60.0;
  private static final int SUB_STEPS = 4;
  private static final int COLLISION_SOLVE_PASSES_PER_SUB_STEP = 9;

  private static int expectedUnorderedNonStaticPairs(int dynCount) {
    // d dynamics, 1 static floor: (d choose 2) + d*1
    if (dynCount < 0) {
      return 0;
    }
    return dynCount * (dynCount - 1) / 2 + dynCount;
  }

  private static int expectedNarrowPhaseCallsPerFrame(int unorderedPairs) {
    return unorderedPairs * SUB_STEPS * COLLISION_SOLVE_PASSES_PER_SUB_STEP;
  }

  private static double semiImplicitSubstepDisplacement(double acceleration) {
    double h = DT / SUB_STEPS;
    return acceleration * h * h * SUB_STEPS * (SUB_STEPS + 1) / 2.0;
  }

  @Test
  void engine_invokesNarrowPhaseOncePerUnorderedPair_twoDynamicPlusFloor() {
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

    // 2 dynamics + 1 static -> (2 choose 2) + 2 = 1 + 2 = 3 unordered pairs.
    assertEquals(expectedNarrowPhaseCallsPerFrame(expectedUnorderedNonStaticPairs(2)),
        counting.getNarrowPhaseCallCount());
  }

  @Test
  void engine_invokesNarrowPhaseOncePerUnorderedPair_fiveIsolatedDynamicBodies() {
    World world = new WorldImpl();
    for (int i = 0; i < 5; i++) {
      // Spaced 50 units — no broad-phase culling, but no overlap either.
      TestBox body = new TestBox(10 + i * 50, 10, 1, 1, 1.0, 0.0, true);
      world.addComponent(body);
    }

    CountingCollisionDetector counting = new CountingCollisionDetector(new CollisionDetectorImpl());
    GameEngineImpl engine = new GameEngineImpl(world, counting);
    engine.step(DT);

    int n = 5;
    assertEquals(expectedNarrowPhaseCallsPerFrame(n * (n - 1) / 2),
        counting.getNarrowPhaseCallCount());
  }

  // --- Tests: broad-phase (one narrow-phase per unordered pair) ---

  @Test
  void firstStepOfFreeFall_matchesSemiImplicitEuler() {
    World world = new WorldImpl();
    TestBox body = new TestBox(0, 100, 1, 1, 1.0, 0.0, true);
    world.addComponent(body);

    CollisionDetector noCollisions = (x, y) -> Optional.empty();
    GameEngineImpl engine = new GameEngineImpl(world, noCollisions);
    engine.step(DT);

    assertEquals(G, body.getDynamicState().getLinearAcceleration().y(), EPS);
    double expectedVy = G * DT;
    assertEquals(expectedVy, body.getDynamicState().getLinearVelocity().y(), EPS);
    // Fixed-step substepping integrates position once per substep.
    double expectedY = 100 + semiImplicitSubstepDisplacement(G);
    assertEquals(expectedY, body.getStaticState().getLinearPosition().y(), EPS);
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
          gravity);
      this.width = width;
      this.height = height;
    }

    @Override
    public void updatePolygon() {
      setPolygon(ConvexPolygon.axisAlignedBox(getX(), getY(), width, height));
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