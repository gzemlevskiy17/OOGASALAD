package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ImpulseSolverUtilities}. These isolate the impulse math from the
 * detection/dispatch pipeline so we can tell whether a landing blow-up comes from the solver itself
 * or from how the engine drives it.
 *
 * <p>Coordinate convention matches the engine: +y points DOWN, gravity is positive y.
 * A body landing on flat ground has penetration pointing upward: from reference (ground) outward
 * toward incident (box), i.e. +(-y) in screen space → {@code new Vector(0, -1) * depth}.
 *
 * <p>Key bugs probed here:
 * <ul>
 *   <li>No separating-velocity clamp in {@code computeImpulse}: if {@code v_rel · n >= 0} the
 *       solver still emits a (negative) impulse, pulling bodies back together. This is the
 *       mechanism by which repeated calls per frame re-glue a body that already bounced.</li>
 *   <li>Running the solver twice on the same {@link Collision} with no state change in between
 *       should be a no-op on the second call. Failing that invariant is what lets the 9×
 *       handleCollisions() loop in {@code GameEngineImpl.step} amplify velocities.</li>
 * </ul>
 */
class ImpulseSolverUtilitiesTest {

  private static final double EPS = 1e-6;
  private static final double BOX_SIZE = 40.0;
  private static final double MASS = 2.0;
  private static final double MOI = MASS * (BOX_SIZE * BOX_SIZE + BOX_SIZE * BOX_SIZE) / 12.0;

  private static DynamicComponent newBox(double x, double y, double restitution) {
    ConvexPolygon poly = ConvexPolygon.axisAlignedBox(
        x - BOX_SIZE / 2, y - BOX_SIZE / 2, BOX_SIZE, BOX_SIZE);
    DynamicComponent c = new DynamicComponent(x, y, 0, poly, "test",
        0.0, MASS, MOI, restitution, false) {
      @Override
      public void updatePolygon() {
        setPolygon(ConvexPolygon.axisAlignedBox(
            getX() - BOX_SIZE / 2, getY() - BOX_SIZE / 2, BOX_SIZE, BOX_SIZE));
      }

      @Override
      public boolean isTrigger() {
        return false;
      }
    };
    c.activate();
    return c;
  }

  private static TerrainBlock newGround(double x, double topY, double restitution) {
    TerrainBlock g = new TerrainBlock(x, topY, 2000, 200, "t.png", false, 0.0, restitution);
    g.activate();
    return g;
  }

  /**
   * Builds a Collision record for a box sitting above ground with {@code depth} pixels of overlap
   * along the vertical axis. Penetration points from reference (ground) up toward incident (box):
   * in engine coords (+y down), that is {@code (0, -depth)}.
   *
   * <p>The manifold has a single contact at the box's bottom edge midpoint.
   */
  private static Collision landingCollisionFor(DynamicComponent box, TerrainBlock ground,
      double depth) {
    Collision c = new Collision(ground, box);
    c.setPenetration(new Vector(0, -depth));
    double contactX = box.getX();
    double contactY = box.getY() + BOX_SIZE / 2;
    Map<Vector, Double> manifold = new HashMap<>();
    manifold.put(new Vector(contactX, contactY), depth);
    c.setManifold(manifold);
    return c;
  }

  /**
   * Ground is a TerrainBlock which has no DynamicState; the solver helper signature demands one for
   * the "reference" slot, so supply an unused zero-valued state.
   */
  private static DynamicState dummyDynamicState() {
    return new DynamicState();
  }

  @Test
  void resolveImpulse_boxFallingIntoGround_zeroRestitution_velocityCancelled() {
    DynamicComponent box = newBox(0, 0, 0.0);
    box.getDynamicState().setLinearVelocity(new Vector(0, 100));
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 1.0, 0.0);
    Collision collision = landingCollisionFor(box, ground, 1.0);

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());

    double vy = box.getDynamicState().getLinearVelocity().y();
    assertEquals(0.0, vy, 1e-3,
        "With e=0, one impulse should kill the downward velocity.");
  }

  @Test
  void resolveImpulse_boxFallingIntoGround_fullRestitution_velocityReversed() {
    DynamicComponent box = newBox(0, 0, 1.0);
    box.getDynamicState().setLinearVelocity(new Vector(0, 100));
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 1.0, 1.0);
    Collision collision = landingCollisionFor(box, ground, 1.0);

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());

    double vy = box.getDynamicState().getLinearVelocity().y();
    assertEquals(-100.0, vy, 1e-3,
        "With e=1, one impulse should flip the downward velocity to an equal-magnitude bounce.");
  }

  /**
   * After one bounce, the box is moving UP (away from the ground). The solver should detect the
   * separating velocity and apply no further impulse — otherwise the 9×-per-frame engine loop
   * repeatedly converts separation back into contact.
   *
   * <p><strong>Expected to FAIL today.</strong> {@code computeImpulse} has no
   * {@code max(0, …)} clamp: when {@code v_rel · n > 0} (already separating) it emits a negative
   * impulse that pulls the bodies back together.
   */
  @Test
  void resolveImpulse_calledTwiceAfterBounce_shouldNotAmplify() {
    DynamicComponent box = newBox(0, 0, 0.5);
    box.getDynamicState().setLinearVelocity(new Vector(0, 100));
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 1.0, 0.5);
    Collision collision = landingCollisionFor(box, ground, 1.0);

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());
    Vector afterFirst = box.getDynamicState().getLinearVelocity();

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());
    Vector afterSecond = box.getDynamicState().getLinearVelocity();

    assertEquals(afterFirst.y(), afterSecond.y(), 1e-3,
        "Second impulse on a now-separating body should be zero. Got "
            + afterFirst.y() + " → " + afterSecond.y());
  }

  /**
   * Eight back-to-back impulse applications (matching {@code JOINT_SOLVER_ITERATIONS}) on the same
   * collision record. If impulses don't clamp on separating velocities, the box's vertical velocity
   * oscillates around zero while drifting — which looks like a landing explosion when compounded
   * with positional correction.
   *
   * <p><strong>Expected to FAIL today.</strong> The final speed is bounded, but each iteration
   * after the first adds non-zero delta velocity: {@code |v_final - v_afterFirst|} should be
   * {@code ~0} and won't be.
   */
  @Test
  void resolveImpulse_nineIterations_shouldConvergeAfterFirstCall() {
    DynamicComponent box = newBox(0, 0, 0.5);
    box.getDynamicState().setLinearVelocity(new Vector(0, 100));
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 1.0, 0.5);
    Collision collision = landingCollisionFor(box, ground, 1.0);

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());
    double afterFirst = box.getDynamicState().getLinearVelocity().y();

    for (int i = 0; i < 8; i++) {
      ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
          collision, ground, box,
          box.getStaticState(), ground.getStaticState(),
          box.getDynamicState(), dummyDynamicState());
    }
    double afterNine = box.getDynamicState().getLinearVelocity().y();

    assertEquals(afterFirst, afterNine, 1e-3,
        "9 solver iterations on the same collision should converge in 1. Got "
            + afterFirst + " → " + afterNine);
  }

  @Test
  void resolveImpulse_warmStartsWithCachedNormalImpulse() {
    DynamicComponent box = newBox(0, 0, 0.0);
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 1.0, 0.0);
    Collision collision = landingCollisionFor(box, ground, 1.0);
    collision.setCachedNormal(new Vector(0, -1));
    collision.setCachedNormalImpulse(20.0);

    ImpulseSolverUtilities.resolveImpulseAtContactsHelper(
        collision, ground, box,
        box.getStaticState(), ground.getStaticState(),
        box.getDynamicState(), dummyDynamicState());

    assertEquals(-10.0, box.getDynamicState().getLinearVelocity().y(), 1e-3);
    assertTrue(collision.getAppliedNormalImpulse() >= 20.0);
  }

  @Test
  void applyPositionalCorrection_staticReference_movesOnlyIncident() {
    DynamicComponent box = newBox(0, 0, 0.5);
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 5.0, 0.0);
    Vector boxPosBefore = box.getStaticState().getLinearPosition();
    Vector groundPosBefore = ground.getStaticState().getLinearPosition();
    Collision collision = landingCollisionFor(box, ground, 5.0);

    ImpulseSolverUtilities.applyPositionalCorrection(collision, ground, box);

    assertEquals(groundPosBefore.x(), ground.getStaticState().getLinearPosition().x(), EPS);
    assertEquals(groundPosBefore.y(), ground.getStaticState().getLinearPosition().y(), EPS);
    double boxDy = box.getStaticState().getLinearPosition().y() - boxPosBefore.y();
    assertTrue(boxDy < 0,
        "Incident box should be pushed upward (more negative y). dy=" + boxDy);
    double expectedMagnitude = (5.0 - 0.01) * 0.4;
    assertEquals(-expectedMagnitude, boxDy, 1e-3,
        "Correction magnitude should be (depth - slop) * 0.4 when only one body is dynamic.");
  }

  @Test
  void applyPositionalCorrection_subSlopDepth_noOp() {
    DynamicComponent box = newBox(0, 0, 0.5);
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 0.005, 0.0);
    Vector posBefore = box.getStaticState().getLinearPosition();
    Collision collision = landingCollisionFor(box, ground, 0.005);

    ImpulseSolverUtilities.applyPositionalCorrection(collision, ground, box);

    assertEquals(posBefore.x(), box.getStaticState().getLinearPosition().x(), EPS);
    assertEquals(posBefore.y(), box.getStaticState().getLinearPosition().y(), EPS);
  }

  @Test
  void applyPositionalCorrection_repeatedCalls_decayGeometrically() {
    DynamicComponent box = newBox(0, 0, 0.5);
    TerrainBlock ground = newGround(-1000, BOX_SIZE / 2 - 10.0, 0.0);

    double depth = 10.0;
    for (int i = 0; i < 20; i++) {
      Collision collision = landingCollisionFor(box, ground, depth);
      double yBefore = box.getStaticState().getLinearPosition().y();
      ImpulseSolverUtilities.applyPositionalCorrection(collision, ground, box);
      double yAfter = box.getStaticState().getLinearPosition().y();
      depth -= (yBefore - yAfter);
    }

    assertTrue(depth < 0.02,
        "20 Baumgarte corrections should bring penetration near slop. Remaining: " + depth);
  }
}
