package oogasalad.model.authoring.internal;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PickupSpec;

/**
 * Normalizes generated pickup entries.
 *
 * @author Emilio Garcia
 */
final class PickupSpecNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a pickup normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  PickupSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns normalized pickup entries.
   *
   * @param pickups requested pickup entries
   * @return normalized pickup entries
   * @author Emilio Garcia
   */
  List<PickupSpec> normalize(List<PickupSpec> pickups) {
    return support.safeList(pickups).stream()
        .filter(Objects::nonNull)
        .map(this::normalizePickup)
        .toList();
  }

  private PickupSpec normalizePickup(PickupSpec pickup) {
    String type = support.normalizeText(pickup.type(), support.s("defaultPickupType")).toLowerCase();
    double width = support.clampPositive(pickup.width(), support.d("defaultPickupWidth"));
    double height = support.clampPositive(pickup.height(), support.d("defaultPickupHeight"));
    double x = support.defaultIfNull(pickup.x(), 0.0);
    double y = support.defaultIfNull(pickup.y(), support.d("defaultPickupY"));
    double amount = support.clamp(
        support.defaultIfNull(pickup.amount(), support.d("defaultPickupAmount")),
        support.d("minPickupAmount"),
        support.d("maxPickupAmount"));
    return new PickupSpec(type, x, y, width, height, amount);
  }
}
