package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.player.internal.PlacementValidator;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlacementValidatorTest {

  private PlacementValidator validator;

  /**
   * Minimal AuthoringPlaceable with a fixed AABB.
   */
  private static AuthoringPlaceable box(double x, double y, double w, double h) {
    return new AuthoringPlaceable() {
      @Override
      public double getWidth() {
        return w;
      }

      @Override
      public double getHeight() {
        return h;
      }

      @Override
      public String getColor() {
        return "";
      }

      @Override
      public String getSprite() {
        return "";
      }

      @Override
      public boolean isTiled() {
        return false;
      }

      @Override
      public double getFriction() {
        return 0;
      }

      @Override
      public double getRestitution() {
        return 0;
      }
    };
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  @BeforeEach
  void setUp() {
    validator = new PlacementValidator();
  }

  // ── no existing objects ───────────────────────────────────────────────────

  @Test
  void emptyCollection_alwaysReturnsFalse() {
    assertFalse(validator.overlapsExistingPlacement(box(0, 0, 10, 10), List.of()));
  }

  // ── clear overlaps ────────────────────────────────────────────────────────

  @Test
  void fullyOverlapping_returnsTrue() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(0, 0, 10, 10);
    assertTrue(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void partialOverlapInBothAxes_returnsTrue() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(5, 5, 10, 10);  // overlaps in bottom-right quadrant
    assertTrue(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void proposedInsideExisting_returnsTrue() {
    AuthoringPlaceable existing = box(0, 0, 100, 100);
    AuthoringPlaceable proposed = box(10, 10, 5, 5);
    assertTrue(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void existingInsideProposed_returnsTrue() {
    AuthoringPlaceable existing = box(10, 10, 5, 5);
    AuthoringPlaceable proposed = box(0, 0, 100, 100);
    assertTrue(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  // ── clear non-overlaps ────────────────────────────────────────────────────

  @Test
  void completelyToTheRight_returnsFalse() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(20, 0, 10, 10);
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void completelyBelow_returnsFalse() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(0, 20, 10, 10);
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void completelyAbove_returnsFalse() {
    AuthoringPlaceable existing = box(0, 10, 10, 10);
    AuthoringPlaceable proposed = box(0, 0, 10, 5);   // ends at y=5, existing starts at y=10
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  // ── touching-edge boundary (strict AABB: shared edge is NOT overlap) ──────

  @Test
  void touchingAtRightEdge_returnsFalse() {
    // proposed.right == existing.left  →  proposed.right (10) == existing.left (10): not strictly <
    AuthoringPlaceable existing = box(10, 0, 10, 10);
    AuthoringPlaceable proposed = box(0, 0, 10, 10);   // proposed.right = 10, existing.left = 10
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void touchingAtBottomEdge_returnsFalse() {
    AuthoringPlaceable existing = box(0, 10, 10, 10);
    AuthoringPlaceable proposed = box(0, 0, 10, 10);   // proposed.bottom = 10, existing.top = 10
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  // ── partial-axis separation ───────────────────────────────────────────────

  @Test
  void overlapsOnlyInX_notInY_returnsFalse() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(5, 20, 10, 10);  // x overlaps but y gap = 20..30 vs 0..10
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  @Test
  void overlapsOnlyInY_notInX_returnsFalse() {
    AuthoringPlaceable existing = box(0, 0, 10, 10);
    AuthoringPlaceable proposed = box(20, 5, 10, 10);  // y overlaps but x gap
    assertFalse(validator.overlapsExistingPlacement(proposed, List.of(existing)));
  }

  // ── multiple existing objects ─────────────────────────────────────────────

  @Test
  void multipleExisting_noneOverlap_returnsFalse() {
    List<AuthoringPlaceable> existing = List.of(
        box(0, 0, 10, 10),
        box(20, 0, 10, 10),
        box(0, 20, 10, 10)
    );
    AuthoringPlaceable proposed = box(100, 100, 5, 5);
    assertFalse(validator.overlapsExistingPlacement(proposed, existing));
  }

  @Test
  void multipleExisting_oneOverlaps_returnsTrue() {
    List<AuthoringPlaceable> existing = List.of(
        box(0, 0, 10, 10),
        box(20, 0, 10, 10),   // proposed overlaps this one
        box(0, 20, 10, 10)
    );
    AuthoringPlaceable proposed = box(25, 5, 5, 5);
    assertTrue(validator.overlapsExistingPlacement(proposed, existing));
  }
}
