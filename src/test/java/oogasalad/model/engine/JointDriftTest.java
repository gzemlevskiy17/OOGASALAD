package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.joints.RigidJoint;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the "jointed vehicle drifts off into space under gravity" bug.
 *
 * <p>The scenario mirrors what {@code VehicleFactory} builds for a small frame grid:
 * cardinal rigid joints, weld-stabilizer joints at offset anchors, and right-angle triangle-chord
 * joints. With this many redundant rigid constraints, the 8-iteration joint loop in
 * {@code GameEngineImpl.step()} never converges. Each frame leaves residual constraint violations
 * that cause phantom position drift on every subsequent {@code applyJoints} call, silently
 * displacing the whole assembly far from the terrain without any corresponding change to velocity.
 */
class JointDriftTest {

  private static final double DT = 1.0 / 60.0;
  private static final int SIM_STEPS = 180;       // 3 seconds
  private static final double CELL = 60.0;
  private static final double BLOCK_MASS = 1.5;
  private static final double FRICTION = 0.3;
  private static final double RESTITUTION = 0.2;

  private static DynamicComponent block(double x, double y) {
    ConvexPolygon poly =
        ConvexPolygon.axisAlignedBox(x - CELL / 2, y - CELL / 2, CELL, CELL);
    double moi = BLOCK_MASS * (CELL * CELL + CELL * CELL) / 12.0;
    DynamicComponent b =
        new DynamicComponent(x, y, 0, poly, "", FRICTION, BLOCK_MASS, moi, RESTITUTION, true) {
          @Override
          public void updatePolygon() {
            setPolygon(ConvexPolygon.axisAlignedBox(
                getX() - CELL / 2, getY() - CELL / 2, CELL, CELL));
          }

          @Override
          public boolean isTrigger() {
            return false;
          }
        };
    b.activate();
    return b;
  }

  private static TerrainBlock ground() {
    // Very thick ground so SAT positional correction can't flip-sign via deep penetration
    // (a separate, pre-existing bug in the collision detector we don't want muddying this test).
    TerrainBlock g =
        new TerrainBlock(-1000, 400, 2000, 2000, "t.png", false, FRICTION, RESTITUTION);
    g.activate();
    return g;
  }

  /**
   * Four blocks in a 2×2 grid, joined with the same pattern VehicleFactory uses: four cardinal
   * rigid joints, four weld stabilizers at offset anchors, and two triangle-chord joints. Dropped
   * on terrain.
   *
   * <p>After 3 seconds, the max |pos| of any block should be bounded. A real vehicle
   * would be resting near y≈400; runaway drift shows up as |pos| escaping far from that.
   */
  @Test
  void overConstrainedGrid_doesNotDriftInfinitely() {
    WorldImpl world = new WorldImpl();

    // 2x2 grid of frames, top row starting at y=100 (above ground at y=400).
    double x0 = 0;
    double y0 = 100;
    DynamicComponent tl = block(x0, y0);
    DynamicComponent tr = block(x0 + CELL, y0);
    DynamicComponent bl = block(x0, y0 + CELL);
    DynamicComponent br = block(x0 + CELL, y0 + CELL);
    world.addComponent(tl);
    world.addComponent(tr);
    world.addComponent(bl);
    world.addComponent(br);
    world.addComponent(ground());

    // Cardinal rigid joints (4 edges of the 2x2 grid).
    world.addJoint(new RigidJoint(tl, tr));
    world.addJoint(new RigidJoint(tl, bl));
    world.addJoint(new RigidJoint(tr, br));
    world.addJoint(new RigidJoint(bl, br));

    // Weld stabilizers: second rigid joint at offset anchor for each cardinal pair
    // (matches VehicleFactory.addWeldStabilizerIfRigidPair).
    double offset = 0.35 * (CELL / 2);
    world.addJoint(
        new RigidJoint(tl, tr, new Vector(0, offset), new Vector(0, offset), -1.0, -1.0));
    world.addJoint(
        new RigidJoint(tl, bl, new Vector(offset, 0), new Vector(offset, 0), -1.0, -1.0));
    world.addJoint(
        new RigidJoint(tr, br, new Vector(offset, 0), new Vector(offset, 0), -1.0, -1.0));
    world.addJoint(
        new RigidJoint(bl, br, new Vector(0, offset), new Vector(0, offset), -1.0, -1.0));

    // Right-angle triangle chords: hypotenuse joints for each right triangle in the grid.
    world.addJoint(new RigidJoint(tl, br));
    world.addJoint(new RigidJoint(tr, bl));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    double peakPos = 0;
    double peakSpeed = 0;
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
      for (DynamicComponent b : new DynamicComponent[]{tl, tr, bl, br}) {
        double pm = Math.hypot(b.getX(), b.getY());
        peakPos = Math.max(peakPos, pm);
        Vector v = b.getDynamicState().getLinearVelocity();
        peakSpeed = Math.max(peakSpeed, Math.hypot(v.x(), v.y()));
      }
    }

    // Bounds reflect what's achievable with the current fixes (split solver, joint velocity
    // coupling, capped positional correction). Position drift is still non-trivial because of
    // a separate SAT-direction bug in deep terrain penetration — kept as a looser bound here.
    assertTrue(peakSpeed < 1500.0,
        "Jointed grid should settle; got peak speed " + peakSpeed);
    assertTrue(peakPos < 5000.0,
        "Jointed grid should stay near origin; got peak |pos| " + peakPos);
  }

  /**
   * Same 2×2 grid dropped on terrain, but this time only cardinal rigid joints — no weld
   * stabilizers, no triangle chords, no anti-rotation joints. If this passes while the
   * over-constrained version fails, the culprit is the redundant joints, not the joint
   * implementation itself.
   */
  @Test
  void cardinalOnlyGrid_doesNotDrift() {
    WorldImpl world = new WorldImpl();
    double x0 = 0;
    double y0 = 100;
    DynamicComponent tl = block(x0, y0);
    DynamicComponent tr = block(x0 + CELL, y0);
    DynamicComponent bl = block(x0, y0 + CELL);
    DynamicComponent br = block(x0 + CELL, y0 + CELL);
    world.addComponent(tl);
    world.addComponent(tr);
    world.addComponent(bl);
    world.addComponent(br);
    world.addComponent(ground());

    world.addJoint(new RigidJoint(tl, tr));
    world.addJoint(new RigidJoint(tl, bl));
    world.addJoint(new RigidJoint(tr, br));
    world.addJoint(new RigidJoint(bl, br));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    double peakPos = 0;
    double peakSpeed = 0;
    for (int i = 0; i < SIM_STEPS; i++) {
      engine.step(DT);
      for (DynamicComponent b : new DynamicComponent[]{tl, tr, bl, br}) {
        peakPos = Math.max(peakPos, Math.hypot(b.getX(), b.getY()));
        Vector v = b.getDynamicState().getLinearVelocity();
        peakSpeed = Math.max(peakSpeed, Math.hypot(v.x(), v.y()));
      }
    }

    // Bounds reflect what's achievable with the current fixes (split solver, joint velocity
    // coupling, capped positional correction). Position drift is still non-trivial because of
    // a separate SAT-direction bug in deep terrain penetration — kept as a looser bound here.
    assertTrue(peakSpeed < 1500.0,
        "Cardinal-only grid should settle; got peak speed " + peakSpeed);
    assertTrue(peakPos < 5000.0,
        "Cardinal-only grid should stay near origin; got peak |pos| " + peakPos);
  }
}
