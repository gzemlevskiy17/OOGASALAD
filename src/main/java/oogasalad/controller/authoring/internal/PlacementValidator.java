package oogasalad.controller.authoring.internal;

import java.util.Collection;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Validates whether a selected placeable can be added to the current authoring world.
 *
 * <p>The current implementation uses rectangular bounds helpers exposed by
 * {@link AuthoringPlaceable}. This keeps overlap geometry with the bounds contract
 * while leaving placement policy decisions in the validator.
 *
 * @author Emilio Garcia
 */
public class PlacementValidator {

  /**
   * Returns true when the proposed backend object would overlap an existing bounded object.
   *
   * @param proposedObject newly created bounded backend object
   * @param existingObjects currently placed bounded objects
   * @return true if the proposed placement intersects an existing bounded object
   */
  public boolean overlapsExistingPlacement(AuthoringPlaceable proposedObject,
      Collection<? extends AuthoringPlaceable> existingObjects) {
    return existingObjects.stream().anyMatch(existingObject -> overlaps(proposedObject, existingObject));
  }

  /**
   * Returns true when the proposed object would overlap any occupied cell in the grid.
   *
   * @param proposedObject newly created bounded backend object
   * @param grid           immutable occupancy snapshot of the current world
   * @return true if the proposed placement intersects an occupied cell
   */
  public boolean overlapsExistingPlacement(AuthoringPlaceable proposedObject, OccupancyGrid grid) {
    int colStart = Math.max(0, (int) proposedObject.left());
    int colEnd = Math.min(grid.getCols(), (int) Math.ceil(proposedObject.right()));
    int rowStart = Math.max(0, (int) proposedObject.top());
    int rowEnd = Math.min(grid.getRows(), (int) Math.ceil(proposedObject.bottom()));

    for (int c = colStart; c < colEnd; c++) {
      for (int r = rowStart; r < rowEnd; r++) {
        if (grid.isOccupied(c, r)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean overlaps(AuthoringPlaceable proposedObject, AuthoringPlaceable existingObject) {
    return proposedObject.left() < existingObject.right()
        && proposedObject.right() > existingObject.left()
        && proposedObject.top() < existingObject.bottom()
        && proposedObject.bottom() > existingObject.top();
  }
}
