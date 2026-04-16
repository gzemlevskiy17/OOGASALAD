package oogasalad.model.player.api;

import java.util.List;
import java.util.Optional;

/**
 * Holds the veihcle grid state from the builder screen to the vehicle factory One PlacedComponent
 * per filled cell. The factory reads this to instantiate the right GameComponent subclasses and
 * wire up the joints.
 */
public record VehicleLayout(List<PlacedComponent> placements) {

  public Optional<String> getComponentAtIndex(int row, int col) {
    for (PlacedComponent p : placements) {
      if (p.row() == row && p.col() == col) {
        return Optional.of(p.componentType());
      }
    }
    return Optional.empty();
  }
}

