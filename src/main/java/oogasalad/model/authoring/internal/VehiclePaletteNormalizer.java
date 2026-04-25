package oogasalad.model.authoring.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.VehiclePaletteSpec;

/**
 * Normalizes generated vehicle-palette metadata.
 *
 * @author Emilio Garcia
 */
final class VehiclePaletteNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a vehicle-palette normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  VehiclePaletteNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns a normalized vehicle-palette specification.
   *
   * @param vehiclePalette requested vehicle-palette data
   * @return normalized vehicle palette
   * @author Emilio Garcia
   */
  VehiclePaletteSpec normalize(VehiclePaletteSpec vehiclePalette) {
    int maxSelectable = vehiclePalette == null || vehiclePalette.maxSelectable() == null
        ? support.n("defaultMaxSelectable")
        : (int) support.clamp(vehiclePalette.maxSelectable(), support.d("minMaxSelectable"),
            support.d("maxMaxSelectable"));

    LinkedHashSet<String> normalizedComponents = new LinkedHashSet<>();
    if (vehiclePalette != null) {
      for (String componentId : vehiclePalette.allowedComponents()) {
        String normalizedId = support.normalizeText(componentId, "");
        if (!normalizedId.isBlank()) {
          normalizedComponents.add(normalizedId);
        }
      }
    }
    return new VehiclePaletteSpec(maxSelectable, List.copyOf(normalizedComponents));
  }
}
