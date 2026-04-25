package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import org.junit.jupiter.api.Test;

class CollisionDetectorImplTest {

  private final CollisionDetectorImpl detector = new CollisionDetectorImpl();

  private static DynamicComponent dynamicBox(double x, double y, double w, double h) {
    return dynamicPolygon(ConvexPolygon.axisAlignedBox(x, y, w, h).points());
  }

  private static DynamicComponent dynamicPolygon(List<Vector> points) {
    ConvexPolygon polygon = new ConvexPolygon(points);
    return new DynamicComponent(0, 0, 0, polygon, "test", 0.3, 1.0, 1.0, 0.2, true) {
      @Override
      public void updatePolygon() {
      }

      @Override
      public boolean isTrigger() {
        return false;
      }
    };
  }

  @Test
  void detectCollision_overlappingAxisAlignedBoxes_returnsCollision() {
    DynamicComponent left = dynamicBox(0, 0, 40, 40);
    DynamicComponent right = dynamicBox(30, 0, 40, 40);

    Optional<Collision> collision = detector.detectCollision(left, right);

    assertTrue(collision.isPresent());
    assertTrue(collision.get().getPenetration().magnitude() > 0);
    assertFalse(collision.get().getCollisionManifold().isEmpty());
  }

  @Test
  void detectCollision_separatedAxisAlignedBoxes_returnsEmpty() {
    DynamicComponent left = dynamicBox(0, 0, 40, 40);
    DynamicComponent right = dynamicBox(120, 0, 40, 40);

    Optional<Collision> collision = detector.detectCollision(left, right);

    assertTrue(collision.isEmpty());
  }

  @Test
  void detectCollision_touchingAtEdge_returnsEmpty() {
    DynamicComponent left = dynamicBox(0, 0, 40, 40);
    DynamicComponent right = dynamicBox(40, 0, 40, 40);

    Optional<Collision> collision = detector.detectCollision(left, right);

    assertTrue(collision.isEmpty());
  }

  /**
   * This is the SAT asymmetry regression: if one polygon's axes are checked but the other's are
   * not, a false positive can slip through for rotated shapes.
   */
  @Test
  void detectCollision_rotatedAndSeparatedOnIncidentAxes_returnsEmpty() {
    DynamicComponent reference = dynamicPolygon(List.of(
        new Vector(0, 20),
        new Vector(20, 0),
        new Vector(60, 40),
        new Vector(40, 60),
        new Vector(0, 20)
    ));
    DynamicComponent incident = dynamicBox(62, 0, 24, 24);

    Optional<Collision> collision = detector.detectCollision(reference, incident);

    assertTrue(collision.isEmpty());
  }
}