package oogasalad.view.authoring.internal.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Reads vehicle palette presentation and catalog defaults from the authoring resource bundle.
 *
 * @author Emilio Garcia
 */
final class VehiclePaletteConfig {

  private static final String BUNDLE_NAME = "oogasalad.view.authoring.VehicleComponentPalette";

  private final ResourceBundle properties;

  private VehiclePaletteConfig(ResourceBundle properties) {
    this.properties = properties;
  }

  static VehiclePaletteConfig loadDefault() {
    return new VehiclePaletteConfig(ResourceBundle.getBundle(BUNDLE_NAME));
  }

  double contentPadding() {
    return d("contentPadding");
  }

  double itemSpacing() {
    return d("itemSpacing");
  }

  double viewportHeight() {
    return d("viewportHeight");
  }

  double cellPrefHeight() {
    return d("cellPrefHeight");
  }

  double cellInset() {
    return d("cellInset");
  }

  double cellLabelWrapWidth() {
    return d("cellLabelWrapWidth");
  }

  double cellIconSize() {
    return d("cellIconSize");
  }

  double cellWidth() {
    return cellLabelWrapWidth() + 2 * cellInset();
  }

  int maxSelectableItems() {
    return Integer.parseInt(properties.getString("maxSelectableItems"));
  }

  String selectionSummaryEmpty() {
    return properties.getString("selectionSummaryEmpty");
  }

  String selectionLimitTitle() {
    return properties.getString("selectionLimitTitle");
  }

  String selectionLimitMessage() {
    return properties.getString("selectionLimitMessage");
  }

  List<String> defaultComponentNames() {
    return csv("componentNames");
  }

  Optional<String> spritePathFor(String componentId) {
    String spriteKey = componentId.replace(" ", "") + ".sprite";
    if (!properties.containsKey(spriteKey)) {
      return Optional.empty();
    }
    return Optional.of(properties.getString(spriteKey));
  }

  private List<String> csv(String key) {
    List<String> result = new ArrayList<>();
    for (String name : properties.getString(key).split(",")) {
      String trimmed = name.trim();
      if (!trimmed.isBlank()) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private double d(String key) {
    return Double.parseDouble(properties.getString(key));
  }
}
