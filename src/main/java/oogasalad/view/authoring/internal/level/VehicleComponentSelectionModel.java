package oogasalad.view.authoring.internal.level;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Maintains available and selected vehicle component IDs for the level authoring palette.
 *
 * @author Emilio Garcia
 */
final class VehicleComponentSelectionModel {

  enum ToggleResult {
    SELECTED,
    DESELECTED,
    REJECTED_LIMIT,
    IGNORED
  }

  private final List<String> defaultComponentIds;
  private final int maxSelectableItems;
  private final LinkedHashMap<String, SelectedVehicleComponent> availableComponents =
      new LinkedHashMap<>();
  private final LinkedHashMap<String, SelectedVehicleComponent> selectedComponents =
      new LinkedHashMap<>();

  VehicleComponentSelectionModel(List<String> defaultComponentIds, int maxSelectableItems) {
    this.defaultComponentIds = List.copyOf(defaultComponentIds);
    this.maxSelectableItems = maxSelectableItems;
    setAvailableComponentIds(null);
  }

  int maxSelectableItems() {
    return maxSelectableItems;
  }

  void setAvailableComponentIds(List<String> componentIds) {
    availableComponents.clear();
    List<String> source = componentIds == null || componentIds.isEmpty()
        ? defaultComponentIds
        : componentIds;
    for (String componentId : source) {
      String sanitized = sanitize(componentId);
      if (!sanitized.isBlank()) {
        availableComponents.putIfAbsent(sanitized,
            new SelectedVehicleComponent(sanitized, sanitized));
      }
    }
    selectedComponents.clear();
  }

  void setSelectedComponentIds(List<String> componentIds) {
    selectedComponents.clear();
    Set<String> selectedIds = new HashSet<>(sanitizedIds(componentIds));
    for (SelectedVehicleComponent component : availableComponents.values()) {
      if (selectedIds.contains(component.id())) {
        selectedComponents.put(component.id(), component);
      }
    }
  }

  ToggleResult toggle(SelectedVehicleComponent component) {
    SelectedVehicleComponent availableComponent = availableComponents.get(component.id());
    if (availableComponent == null) {
      return ToggleResult.IGNORED;
    }
    if (selectedComponents.remove(component.id()) != null) {
      return ToggleResult.DESELECTED;
    }
    if (selectedComponents.size() >= maxSelectableItems) {
      return ToggleResult.REJECTED_LIMIT;
    }
    selectedComponents.put(availableComponent.id(), availableComponent);
    return ToggleResult.SELECTED;
  }

  boolean isSelected(String componentId) {
    return selectedComponents.containsKey(componentId);
  }

  List<SelectedVehicleComponent> availableComponents() {
    return List.copyOf(availableComponents.values());
  }

  List<SelectedVehicleComponent> selectedComponents() {
    return List.copyOf(selectedComponents.values());
  }

  List<String> selectedIds() {
    return selectedComponents.values().stream()
        .map(SelectedVehicleComponent::id)
        .toList();
  }

  private List<String> sanitizedIds(List<String> componentIds) {
    if (componentIds == null) {
      return List.of();
    }
    return componentIds.stream()
        .map(this::sanitize)
        .filter(id -> !id.isBlank())
        .toList();
  }

  private String sanitize(String componentId) {
    return componentId == null ? "" : componentId.trim();
  }
}
