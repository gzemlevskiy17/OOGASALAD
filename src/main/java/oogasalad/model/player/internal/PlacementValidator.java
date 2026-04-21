package oogasalad.model.player.internal;

import java.util.Optional;
import oogasalad.model.player.api.BuildRules;
import oogasalad.model.player.api.BuildRulesRegistry;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.PositionConstraint;
import oogasalad.model.player.api.Side;
import oogasalad.model.player.api.ValidationResult;
import oogasalad.model.player.api.VehicleLayout;

/**
 * Checks whether a candidate placement satisfies its component type's {@link BuildRules}. The
 * validator enforces four things: grid-position constraints (e.g. wheels in the bottom row),
 * containment (e.g. pigs inside a frame), required open sides (e.g. a fan's intake must face empty
 * space), and required attachments (e.g. a fan's right must meet a neighbor whose left is
 * attachable).
 *
 * <p>An attachment is valid when both components list the shared edge in their
 * {@code connectionSurfaces}: the candidate offers the side, and the neighbor offers the opposite
 * side. Adjacency without a declared connection is still allowed — two wheels sitting next to each
 * other pass validation because neither demands the shared edge.
 *
 * @author Paulina Vvedenskaya
 */
public class PlacementValidator {

  private static final String FRAME_TYPE = "Frame";

  private final BuildRulesRegistry registry;
  private final int gridRows;
  private final int gridCols;

  public PlacementValidator(BuildRulesRegistry registry, int gridRows, int gridCols) {
    this.registry = registry;
    this.gridRows = gridRows;
    this.gridCols = gridCols;
  }

  public ValidationResult canPlace(VehicleLayout layout, PlacedComponent candidate,
      int rotationQuarters) {
    BuildRules rules = registry.lookup(candidate.componentType());
    int row = candidate.row();
    int col = candidate.col();
    String type = candidate.componentType();

    if (rules.position() == PositionConstraint.BOTTOM_ROW_OF_GRID && row != gridRows - 1) {
      return ValidationResult.fail(type + " must be placed in the bottom row");
    }
    if (rules.containment() == Containment.INSIDE_FRAME && !frameAt(layout, row, col)) {
      return ValidationResult.fail(type + " must be placed inside a Frame");
    }
    for (Side side : rules.rotatedRequiredOpenSides(rotationQuarters)) {
      if (neighborType(layout, row, col, side).isPresent()) {
        return ValidationResult.fail(type + " requires open space on " + side);
      }
    }
    for (Side side : rules.rotatedRequiredAttachments(rotationQuarters)) {
      Optional<String> neighbor = neighborType(layout, row, col, side);
      if (neighbor.isEmpty()) {
        return ValidationResult.fail(type + " must attach to a neighbor on " + side);
      }
      BuildRules neighborRules = registry.lookup(neighbor.get());
      if (!neighborRules.connectionSurfaces().contains(side.opposite())) {
        return ValidationResult.fail(
            type + " cannot attach to " + neighbor.get() + " on " + side);
      }
    }
    return ValidationResult.ok();
  }

  private boolean frameAt(VehicleLayout layout, int row, int col) {
    return layout.placements().stream()
        .anyMatch(p -> p.row() == row && p.col() == col && FRAME_TYPE.equals(p.componentType()));
  }

  private Optional<String> neighborType(VehicleLayout layout, int row, int col, Side side) {
    int r = row + switch (side) {
      case TOP -> -1;
      case BOTTOM -> 1;
      default -> 0;
    };
    int c = col + switch (side) {
      case LEFT -> -1;
      case RIGHT -> 1;
      default -> 0;
    };
    if (r < 0 || r >= gridRows || c < 0 || c >= gridCols) {
      return Optional.empty();
    }
    return layout.getComponentAtIndex(r, c);
  }
}
