package oogasalad.model.engine.internal.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;
import org.junit.jupiter.api.Test;

/**
 * Tests named external force accumulation on DynamicComponent.
 *
 * @author Paulina Vvedenskaya
 */
class ExternalForceTest {

  private static final double EPS = 1e-9;

  private static DynamicComponent newBody() {
    ConvexPolygon box = ConvexPolygon.axisAlignedBox(0, 0, 10, 10);
    return new DynamicComponent(5, 5, 0, box, "", 0, 1.0, 1.0, 0.5, false) {
    };
  }

  @Test
  void addExternalForce_singleForce_accumulatedInTotal() {
    DynamicComponent body = newBody();

    body.addExternalForce("thrust_left", new Vector(-400, 0));

    Vector total = body.getExternalForceTotal();
    assertEquals(-400, total.x(), EPS);
    assertEquals(0, total.y(), EPS);
  }

  @Test
  void addExternalForce_twoForces_summedCorrectly() {
    DynamicComponent body = newBody();

    body.addExternalForce("thrust_left", new Vector(-400, 0));
    body.addExternalForce("jump", new Vector(0, 500));

    Vector total = body.getExternalForceTotal();
    assertEquals(-400, total.x(), EPS);
    assertEquals(500, total.y(), EPS);
  }

  @Test
  void removeExternalForce_existingForce_removedFromTotal() {
    DynamicComponent body = newBody();
    body.addExternalForce("thrust_left", new Vector(-400, 0));
    body.addExternalForce("jump", new Vector(0, 500));

    body.removeExternalForce("thrust_left");

    Vector total = body.getExternalForceTotal();
    assertEquals(0, total.x(), EPS);
    assertEquals(500, total.y(), EPS);
  }

  @Test
  void removeExternalForce_nonExistentId_noEffect() {
    DynamicComponent body = newBody();
    body.addExternalForce("thrust_left", new Vector(-400, 0));

    body.removeExternalForce("nonexistent");

    Vector total = body.getExternalForceTotal();
    assertEquals(-400, total.x(), EPS);
  }

  @Test
  void addExternalForce_sameIdTwice_replacesOldForce() {
    DynamicComponent body = newBody();
    body.addExternalForce("thrust", new Vector(100, 0));

    body.addExternalForce("thrust", new Vector(200, 0));

    Vector total = body.getExternalForceTotal();
    assertEquals(200, total.x(), EPS);
  }

  @Test
  void clearExternalForces_removesAll() {
    DynamicComponent body = newBody();
    body.addExternalForce("a", new Vector(100, 0));
    body.addExternalForce("b", new Vector(0, 200));

    body.clearExternalForces();

    Vector total = body.getExternalForceTotal();
    assertEquals(0, total.x(), EPS);
    assertEquals(0, total.y(), EPS);
  }

  @Test
  void getExternalForceTotal_noForces_returnsZero() {
    DynamicComponent body = newBody();

    Vector total = body.getExternalForceTotal();

    assertEquals(0, total.x(), EPS);
    assertEquals(0, total.y(), EPS);
  }

  @Test
  void addExternalForce_onSleepingBody_wakesBody() {
    DynamicComponent body = newBody();
    body.setSleepingAllowed(true);
    body.setSleeping(true);
    assertTrue(body.isSleeping());

    body.addExternalForce("push", new Vector(100, 0));

    assertTrue(!body.isSleeping(),
        "Adding a non-zero external force should wake a sleeping body.");
  }
}
