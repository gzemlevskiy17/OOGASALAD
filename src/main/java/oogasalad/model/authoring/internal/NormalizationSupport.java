package oogasalad.model.authoring.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared resource-backed helpers used by generated-level normalization collaborators.
 *
 * @author Emilio Garcia
 */
final class NormalizationSupport {

  private final ResourceBundle props;

  /**
   * Creates helper accessors backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  NormalizationSupport(ResourceBundle props) {
    this.props = props;
  }

  String s(String key) {
    return props.getString(key);
  }

  double d(String key) {
    return Double.parseDouble(props.getString(key));
  }

  int n(String key) {
    return Integer.parseInt(props.getString(key));
  }

  boolean b(String key) {
    return Boolean.parseBoolean(props.getString(key));
  }

  String normalizeText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? fallback : trimmed;
  }

  <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }

  List<Double> csvDoubles(String key) {
    return List.of(props.getString(key).split(",")).stream()
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(Double::parseDouble)
        .toList();
  }

  List<String> csvStrings(String key) {
    return List.of(props.getString(key).split(",")).stream()
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .toList();
  }

  double defaultIfNull(Double value, double fallback) {
    return value == null ? fallback : value;
  }

  int clampInt(Integer value, int fallback, int min, int max) {
    int resolved = value == null ? fallback : value;
    return Math.max(min, Math.min(max, resolved));
  }

  double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  double clampPositive(Double value, double fallback) {
    if (value == null || value <= 0) {
      return fallback;
    }
    return value;
  }

  Set<String> csvSet(String key) {
    return List.of(props.getString(key).split(",")).stream()
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(String::toLowerCase)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
