package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import org.junit.jupiter.api.Test;

/**
 * Tests that the ForceIntegrator applies named external forces from DynamicComponent.
 *
 * @author Paulina Vvedenskaya
 */
class ForceIntegratorExternalForceTest {

  private static final double EPS = 1e-6;

  @Test
  void applyForces_componentWithExternalForce_includesExternalForceInAccumulation() {
    WorldImpl world = new WorldImpl(new PhysicsEnvironment(new Vector(0, 0), 0));
    PigComponent pig = newPig();
    pig.setAffectedByGravity(true);
    world.addComponent(pig);

    pig.addExternalForce("jump", new Vector(0, 500));

    ForceIntegrator integrator = new ForceIntegrator(world);
    integrator.applyForces(List.of(pig));

    Vector force = pig.getForce();
    assertEquals(0, force.x(), EPS, "No horizontal force expected.");
    assertEquals(500, force.y(), EPS,
        "External force should be included in accumulated force.");
  }

  @Test
  void applyForces_externalForceRemoved_noLongerApplied() {
    WorldImpl world = new WorldImpl(new PhysicsEnvironment(new Vector(0, 0), 0));
    PigComponent pig = newPig();
    pig.setAffectedByGravity(true);
    world.addComponent(pig);

    pig.addExternalForce("jump", new Vector(0, 500));
    pig.removeExternalForce("jump");

    ForceIntegrator integrator = new ForceIntegrator(world);
    integrator.resetForces(List.of(pig));
    integrator.applyForces(List.of(pig));

    Vector force = pig.getForce();
    assertEquals(0, force.x(), EPS);
    assertEquals(0, force.y(), EPS,
        "After removing external force, no external force should be applied.");
  }

  private PigComponent newPig() {
    return new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-30, -30, 60, 60),
        "", 0.3, 10.0, 1.0, 0.5, true, 100, "TestPig");
  }
}
