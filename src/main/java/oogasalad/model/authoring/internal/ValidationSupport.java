package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared validation helpers used by schema-native generated-level validators.
 * @author Emilio Garcia
 */
final class ValidationSupport {

  private static final Logger LOG = LogManager.getLogger(ValidationSupport.class);

  private final ResourceBundle props;

  /**
   * Creates helper accessors backed by the given validation properties bundle.
   *
   * @param props validator properties bundle
   */
  ValidationSupport(ResourceBundle props) {
    this.props = props;
  }

  void requirePresent(String key, Object value, Object... args) {
    if (value == null) {
      fail(key, args);
    }
  }

  void requireNonBlank(String key, String value, Object... args) {
    if (value == null || value.isBlank()) {
      fail(key, args);
    }
  }

  void validateRange(String key, Double value, double min, double max, Object... args) {
    if (value == null || value < min || value > max) {
      fail(key, appendRangeArgs(args, min, max));
    }
  }

  void validateIntegerRange(String key, Integer value, int min, int max, Object... args) {
    if (value == null || value < min || value > max) {
      fail(key, appendRangeArgs(args, min, max));
    }
  }

  void fail(String key, Object... args) {
    String message = String.format(props.getString(key), args);
    LOG.warn(message);
    throw new GeneratedLevelSpecValidationException(message);
  }

  double d(String key) {
    return Double.parseDouble(props.getString(key));
  }

  int n(String key) {
    return Integer.parseInt(props.getString(key));
  }

  String s(String key) {
    return props.getString(key);
  }

  private Object[] appendRangeArgs(Object[] args, Object min, Object max) {
    Object[] expandedArgs = new Object[args.length + 2];
    System.arraycopy(args, 0, expandedArgs, 0, args.length);
    expandedArgs[args.length] = min;
    expandedArgs[args.length + 1] = max;
    return expandedArgs;
  }
}
