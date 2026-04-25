package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.api.RecordingPickupEffect;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import org.junit.jupiter.api.Test;

/**
 * Test that PassThrough does not change the state of the model
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
class DynamicPassthroughSolverTest {

  private static final double EPS = 1e-9;

  private static ConvexPolygon unitSquare() {
    return new ConvexPolygon(
        List.of(
            new Vector(0, 0),
            new Vector(1, 0),
            new Vector(1, 1),
            new Vector(0, 1)));
  }

  private static DynamicComponent newDynamic(double x, double y) {
    DynamicComponent body =
        new DynamicComponent(x, y, Math.PI / 6, unitSquare(), "dyn", 0.2, 2.5, 3.0, 0.4, true) {
        };
    body.activate();
    body.getDynamicState().setLinearVelocity(new Vector(3.0, -1.5));
    body.getDynamicState().setAngularVelocity(0.7);
    body.getDynamicState().setLinearAcceleration(new Vector(0.1, -0.2));
    body.getDynamicState().setAngularAcceleration(-0.05);
    body.setForce(new Vector(0.5, 0.25));
    return body;
  }

  private static PassThroughComponent newPassThrough(double x, double y) {
    return new PassThroughComponent(x, y, -0.3, unitSquare(), "pt") {
    };
  }

  private static DynamicSnapshot snapshotDynamic(DynamicComponent d) {
    var s = d.getDynamicState();
    return new DynamicSnapshot(
        d.x(),
        d.y(),
        d.rotation(),
        s.getLinearVelocity().x(),
        s.getLinearVelocity().y(),
        s.getAngularVelocity(),
        s.getLinearAcceleration().x(),
        s.getLinearAcceleration().y(),
        s.getAngularAcceleration(),
        d.getMass(),
        d.getFriction(),
        d.getRestitution(),
        d.getMomentOfInertia(),
        d.getForce().x(),
        d.getForce().y(),
        d.isAffectedByGravity(),
        d.getPolygon(),
        d.getTag());
  }

  private static PassThroughSnapshot snapshotPassThrough(PassThroughComponent p) {
    var st = p.getStaticState();
    return new PassThroughSnapshot(
        st.getLinearPosition().x(),
        st.getLinearPosition().y(),
        st.getAngularPosition(),
        p.getPolygon(),
        p.getTag(),
        p.isActive());
  }

  private static PickupScenario newPickupScenario() {
    DynamicComponent dynamic = newDynamic(1.0, 2.0);
    RecordingPickupEffect effect = new RecordingPickupEffect();
    PickupComponent pickup = new PickupComponent(4.0, 5.0, 0, unitSquare(), "", "Test", effect);
    Collision collision = new Collision(dynamic, pickup);
    return new PickupScenario(dynamic, pickup, effect, collision);
  }

  @Test
  void solve_pickup_invokesEffectExactlyOnce() {
    PickupScenario s = newPickupScenario();

    new DynamicPassthroughSolver().solve(s.dynamic(), s.pickup(), s.collision());

    assertEquals(1, s.effect().callCount());
  }

  @Test
  void solve_pickup_passesDynamicAsTargetToEffect() {
    PickupScenario s = newPickupScenario();

    new DynamicPassthroughSolver().solve(s.dynamic(), s.pickup(), s.collision());

    assertSame(s.dynamic(), s.effect().lastTarget());
  }

  @Test
  void solve_pickup_marksPickupConsumed() {
    PickupScenario s = newPickupScenario();

    new DynamicPassthroughSolver().solve(s.dynamic(), s.pickup(), s.collision());

    assertTrue(s.pickup().isConsumed());
  }

  @Test
  void solve_pickup_deactivatesPickup() {
    PickupScenario s = newPickupScenario();

    new DynamicPassthroughSolver().solve(s.dynamic(), s.pickup(), s.collision());

    assertFalse(s.pickup().isActive());
  }

  @Test
  void solve_pickup_doesNotMutateDynamicState() {
    PickupScenario s = newPickupScenario();
    DynamicSnapshot before = snapshotDynamic(s.dynamic());

    new DynamicPassthroughSolver().solve(s.dynamic(), s.pickup(), s.collision());

    assertEquals(before, snapshotDynamic(s.dynamic()));
  }

  @Test
  void solve_repeatedCallsOnSamePickupInvokeEffectOnlyOnce() {
    PickupScenario s = newPickupScenario();
    DynamicPassthroughSolver solver = new DynamicPassthroughSolver();

    solver.solve(s.dynamic(), s.pickup(), s.collision());
    solver.solve(s.dynamic(), s.pickup(), s.collision());

    assertEquals(1, s.effect().callCount());
  }

  @Test
  void solve_doesNotChangeDynamicOrPassThroughState() {
    DynamicComponent dynamic = newDynamic(1.0, 2.0);
    PassThroughComponent passThrough = newPassThrough(4.0, 5.0);

    Collision collision = new Collision(dynamic, passThrough);
    collision.setPenetration(new Vector(0.05, -0.02));
    collision.setReferenceEdge(new Vector[]{new Vector(0, 0), new Vector(1, 0)});
    collision.setIncidentEdge(new Vector[]{new Vector(0, 1), new Vector(1, 1)});

    DynamicSnapshot beforeDyn = snapshotDynamic(dynamic);
    PassThroughSnapshot beforePass = snapshotPassThrough(passThrough);
    Vector pen = collision.getPenetration();
    double penX = pen.x();
    double penY = pen.y();

    new DynamicPassthroughSolver().solve(dynamic, passThrough, collision);

    assertEquals(beforeDyn, snapshotDynamic(dynamic), "dynamic body state must be unchanged");
    assertEquals(beforePass, snapshotPassThrough(passThrough),
        "passthrough body state must be unchanged");
    assertEquals(penX, collision.getPenetration().x(), EPS);
    assertEquals(penY, collision.getPenetration().y(), EPS);
    assertSame(beforeDyn.polygon(), dynamic.getPolygon());
    assertSame(beforePass.polygon(), passThrough.getPolygon());
  }

  private record PickupScenario(DynamicComponent dynamic, PickupComponent pickup,
                                RecordingPickupEffect effect, Collision collision) {

  }

  private record DynamicSnapshot(
      double x,
      double y,
      double rotation,
      double vx,
      double vy,
      double angularVel,
      double ax,
      double ay,
      double angularAccel,
      double mass,
      double friction,
      double restitution,
      double momentOfInertia,
      double fx,
      double fy,
      boolean affectedByGravity,
      ConvexPolygon polygon,
      String tag) {

  }

  private record PassThroughSnapshot(
      double x,
      double y,
      double rotation,
      ConvexPolygon polygon,
      String tag,
      boolean active) {

  }
}