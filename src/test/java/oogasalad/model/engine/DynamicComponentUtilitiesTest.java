package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.RelativeDirectionDriveComponent;
import org.junit.jupiter.api.Test;

/**
 * Tests for FORCE_APPLICATION_BUTTON_BUG (see doc/bugs/FORCE_APPLICATION_BUTTON_BUG.md).
 * Authored gameplay buttons ultimately call DynamicComponent.addExternalForce; those
 * vectors must be merged into the per-step force accumulation in
 * DynamicComponentUtilities.applyForces so the vehicle responds. Without that wiring,
 * buttons may appear and update the external-force map, but the physics integrator never sees them.
 *
 * @author Iurii Beliaev
 */
class DynamicComponentUtilitiesTest {

  private static final double EPS = 1e-9;

  @Test
  void applyForces_includesExternalForceTotalInAccumulator_nonDriveBody() {
    WorldImpl world = new WorldImpl(new PhysicsEnvironment(new Vector(0, 0), 0));
    DynamicComponent body = nonGravitatingBody();
    body.addExternalForce("authoring:Push", new Vector(420, 0));
    DynamicComponentUtilities.resetForces(List.of(body));
    DynamicComponentUtilities.applyForces(List.of(body), world);
    Vector accumulated = body.getForce();
    assertEquals(
        420,
        accumulated.x(),
        EPS,
        "Per-step force application must include getExternalForceTotal() so catalog button forces move the body.");
    assertEquals(0, accumulated.y(), EPS);
  }
  @Test
  void applyForces_sumsMultipleExternalForcesIntoAccumulator() {
    WorldImpl world = new WorldImpl(new PhysicsEnvironment(new Vector(0, 0), 0));
    DynamicComponent body = nonGravitatingBody();
    body.addExternalForce("a", new Vector(100, 0));
    body.addExternalForce("b", new Vector(50, -30));
    DynamicComponentUtilities.resetForces(List.of(body));
    DynamicComponentUtilities.applyForces(List.of(body), world);
    Vector accumulated = body.getForce();
    assertEquals(150, accumulated.x(), EPS);
    assertEquals(-30, accumulated.y(), EPS);
  }
  @Test
  void applyForces_includesExternalForceForDriveComponentWhenDriveOff() {
    WorldImpl world = new WorldImpl(new PhysicsEnvironment(new Vector(0, 0), 0));
    RelativeDirectionDriveComponent fan = new RelativeDirectionDriveComponent(
        0, 0, 0, 10.0, 0.3, 100.0, "Fan", 60.0, 60.0, new Vector(1, 0));
    fan.setThrustForce(3000);
    fan.addExternalForce("button:lift", new Vector(0, -2500));
    DynamicComponentUtilities.resetForces(List.of(fan));
    DynamicComponentUtilities.applyForces(List.of(fan), world);
    Vector accumulated = fan.getForce();
    assertEquals(
        0,
        accumulated.x(),
        EPS,
        "With drive disabled, x force should come only from external/catalog forces (none here).");
    assertEquals(
        -2500,
        accumulated.y(),
        EPS,
        "Catalog-style upward force on a drive part must still enter the force accumulator each step.");
  }
  private static DynamicComponent nonGravitatingBody() {
    ConvexPolygon box = ConvexPolygon.axisAlignedBox(0, 0, 10, 10);
    return new DynamicComponent(5, 5, 0, box, "", 0, 1.0, 1.0, 0.5, false) {};
  }
}