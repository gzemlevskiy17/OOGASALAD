package oogasalad.view.authoring.internal.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests vehicle component selection state independently from JavaFX palette rendering.
 *
 * @author Emilio Garcia
 */
class VehicleComponentSelectionModelTest {

  @Test
  void setAvailableComponentIds_nullInputUsesDefaultComponents() {
    VehicleComponentSelectionModel model =
        new VehicleComponentSelectionModel(List.of("Pig", "Frame"), 2);

    model.setAvailableComponentIds(null);

    assertEquals(List.of("Pig", "Frame"), availableIds(model));
  }

  @Test
  void setAvailableComponentIds_trimsAndDeduplicatesComponentIds() {
    VehicleComponentSelectionModel model =
        new VehicleComponentSelectionModel(List.of("Pig"), 2);

    model.setAvailableComponentIds(List.of(" Frame ", "Frame", "", "Wheel"));

    assertEquals(List.of("Frame", "Wheel"), availableIds(model));
  }

  @Test
  void setSelectedComponentIds_unknownIdsIgnoredAndPaletteOrderPreserved() {
    VehicleComponentSelectionModel model =
        new VehicleComponentSelectionModel(List.of("Pig"), 3);
    model.setAvailableComponentIds(List.of("Frame", "Wheel", "Engine"));

    model.setSelectedComponentIds(List.of("Engine", "Missing", "Frame"));

    assertEquals(List.of("Frame", "Engine"), model.selectedIds());
  }

  @Test
  void toggle_selectedComponentAgainDeselectsIt() {
    VehicleComponentSelectionModel model =
        new VehicleComponentSelectionModel(List.of("Frame"), 1);
    SelectedVehicleComponent frame = new SelectedVehicleComponent("Frame", "Frame");

    assertEquals(VehicleComponentSelectionModel.ToggleResult.SELECTED, model.toggle(frame));
    assertEquals(VehicleComponentSelectionModel.ToggleResult.DESELECTED, model.toggle(frame));

    assertEquals(List.of(), model.selectedIds());
  }

  @Test
  void toggle_selectionLimitReachedRejectsAdditionalComponent() {
    VehicleComponentSelectionModel model =
        new VehicleComponentSelectionModel(List.of("Frame", "Wheel", "Engine"), 2);

    assertEquals(VehicleComponentSelectionModel.ToggleResult.SELECTED,
        model.toggle(new SelectedVehicleComponent("Frame", "Frame")));
    assertEquals(VehicleComponentSelectionModel.ToggleResult.SELECTED,
        model.toggle(new SelectedVehicleComponent("Wheel", "Wheel")));
    assertEquals(VehicleComponentSelectionModel.ToggleResult.REJECTED_LIMIT,
        model.toggle(new SelectedVehicleComponent("Engine", "Engine")));

    assertEquals(List.of("Frame", "Wheel"), model.selectedIds());
  }

  private List<String> availableIds(VehicleComponentSelectionModel model) {
    return model.availableComponents().stream()
        .map(SelectedVehicleComponent::id)
        .toList();
  }
}
