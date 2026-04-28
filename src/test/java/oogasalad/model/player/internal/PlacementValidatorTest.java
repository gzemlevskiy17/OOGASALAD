package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import org.junit.jupiter.api.Test;

class PlacementValidatorTest {

  private final DefaultBuildRulesRegistry registry = new DefaultBuildRulesRegistry();

  @Test
  void pigInsideFrame_valid() {
    PlacementValidator v = new PlacementValidator(registry, 5, 5);
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 2, "Frame")));
    assertTrue(v.canPlace(layout, new PlacedComponent(2, 2, "Pig"), 0).valid());
  }

  @Test
  void pigAdjacentToFrame_valid() {
    PlacementValidator v = new PlacementValidator(registry, 5, 5);
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame")));
    assertTrue(v.canPlace(layout, new PlacedComponent(2, 2, "Pig"), 0).valid());
  }

  @Test
  void pigFloatingWithNoVehicle_invalid() {
    PlacementValidator v = new PlacementValidator(registry, 5, 5);
    assertFalse(v.canPlace(new VehicleLayout(List.of()), new PlacedComponent(2, 2, "Pig"), 0)
        .valid());
  }

  @Test
  void pigOnlyNeighbor_invalid() {
    PlacementValidator v = new PlacementValidator(registry, 5, 5);
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Pig")));
    assertFalse(v.canPlace(layout, new PlacedComponent(2, 2, "Pig"), 0).valid());
  }
}